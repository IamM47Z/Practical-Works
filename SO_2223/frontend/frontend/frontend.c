#include <stdio.h>
#include <errno.h>
#include <string.h>
#include <stdlib.h>
#include <libgen.h>
#include <unistd.h>
#include <sys/stat.h>

#ifdef __APPLE__

#include <mach-o/dyld.h>

#endif

#include "frontend.h"

void processHeartbeat( PFRONTEND pfrontend )
{
	while ( pfrontend )
	{
		pthread_setcancelstate( PTHREAD_CANCEL_ENABLE, NULL );
		
		sleep( pfrontend->heartbeat );
		
		pthread_setcancelstate( PTHREAD_CANCEL_DISABLE, NULL );
		
		if ( !pfrontend->logged_in )
			break;
		
		REQUEST req;
		memset( &req, 0, sizeof( REQUEST ) );
		req.from = getpid( );
		req.to = 0;
		req.type = REQ_HEARTBEAT;
		
		if ( writeIoFifo( &pfrontend->server_io, ( char* ) &req, sizeof( req ) ) == -1 )
		{
			perror( "Error processing Heartbeat" );
			exit( 1 );
		}
	}
}

PFRONTEND createFrontend( char* username, char* password )
{
	if ( !username || !password || strlen( username ) > 24 || strlen( password ) > 24 )
		return NULL;
	
	char* str_heartbeat = getenv( "HEARTBEAT" );
	if ( !str_heartbeat )
		return NULL;
	
	PFRONTEND pfrontend = malloc( sizeof( FRONTEND ) );
	if ( !pfrontend )
		return NULL;
	
	int heartbeat = ( int ) strtol( str_heartbeat, NULL, 10 );
	if ( heartbeat <= 0 )
	{
		perror( "Invalid HeartBeat value" );
		free( pfrontend );
		return NULL;
	}
	
	// get executable directory path
	char full_path[PATH_MAX];
#ifdef __APPLE__
	uint32_t size = PATH_MAX;
	if ( _NSGetExecutablePath( full_path, &size ) == 0 )
#else
		int bytes = readlink( "/proc/self/exe", full_path, PATH_MAX );
		if ( bytes > 0 )
#endif
		strcpy( pfrontend->pathname, dirname( full_path ) );
	else
	{
		perror( "Error obtaining executable path!" );
		free( pfrontend );
		return NULL;
	}
	
	pfrontend->heartbeat = heartbeat;
	
	strcpy( pfrontend->username, username );
	strcpy( pfrontend->password, password );
	
	pfrontend->logged_in = false;
	
	if ( !initializeIo( &pfrontend->server_io, "AuctionServer", O_WRONLY ) )
	{
		if ( errno == ENOENT )
			printf( "The backend must be running!" );
		else
			perror( "Error Accessing Server Fifo" );
		
		free( pfrontend );
		return NULL;
	}
	
	sprintf( pfrontend->pathname, "AuctionClient%d", getpid( ) );
	
	if ( mkfifo( pfrontend->pathname, 0666 ) == -1 )
	{
		perror( "Error creating Fifo" );
		free( pfrontend );
		return NULL;
	}
	
	return pfrontend;
}

void deleteFrontend( PFRONTEND pfrontend )
{
	if ( !pfrontend )
		return;
	
	if ( pfrontend->logged_in )
	{
		logoutFrontend( pfrontend );
		
		REQUEST req;
		memset( &req, 0, sizeof( REQUEST ) );
		req.from = getpid( );
		req.to = 0;
		req.type = REQ_DISCONNECT;
		
		if ( writeIoFifo( &pfrontend->server_io, ( char* ) &req, sizeof( req ) ) == -1 )
			perror( "Error writing to Server Fifo" );
	}
	
	if ( pfrontend->server_io.initialized )
		destroyIo( &pfrontend->server_io );
	
	unlink( pfrontend->pathname );
	
	memset( pfrontend, 0, sizeof( FRONTEND ) );
	free( pfrontend );
}

bool loginFrontend( PFRONTEND pfrontend )
{
	if ( !pfrontend || pfrontend->logged_in )
		return false;
	
	REQUEST req;
	memset( &req, 0, sizeof( REQUEST ) );
	req.from = getpid( );
	req.to = 0;
	req.type = REQ_LOGIN;
	
	strcpy( req.response.front.login.pathname, pfrontend->pathname );
	strcpy( req.response.front.login.username, pfrontend->username );
	strcpy( req.response.front.login.password, pfrontend->password );
	
	if ( writeIoFifo( &pfrontend->server_io, ( char* ) &req, sizeof( req ) ) == -1 )
	{
		perror( "Error writing to Server Fifo" );
		return false;
	}
	
	if ( initializeIo( &pfrontend->client_io, pfrontend->pathname, O_RDONLY ) &&
	     readIoFifo( &pfrontend->client_io, ( char* ) &req, sizeof( req ) ) != -1 )
	{
		if ( toggleIoBlockable( &pfrontend->client_io ) )
		{
			pfrontend->logged_in = req.response.status;
			
			if ( pfrontend->logged_in )
			{
				if ( pthread_create( &pfrontend->heartbeat_thread, NULL, ( void* ( * )( void* ) ) processHeartbeat,
				                     pfrontend ) )
				{
					perror( "Error creating Heartbeat Thread" );
					exit( 1 );
				}
				
				printf( "Logged in as %s\n", pfrontend->username );
			}
			else
				printf( "Invalid user/password or already logged in!\n" );
			
			return pfrontend->logged_in;
		}
		
		perror( "Error changing IO Flags" );
		return false;
	}
	
	perror( "Error reading IO" );
	return false;
}

bool logoutFrontend( PFRONTEND pfrontend )
{
	if ( !pfrontend || !pfrontend->logged_in )
		return false;
	
	if ( pfrontend->heartbeat_thread )
		if ( !pthread_cancel( pfrontend->heartbeat_thread ) )
			pthread_join( pfrontend->heartbeat_thread, NULL );
	
	if ( pfrontend->client_io.initialized )
		destroyIo( &pfrontend->client_io );
	
	pfrontend->logged_in = false;
	return true;
}