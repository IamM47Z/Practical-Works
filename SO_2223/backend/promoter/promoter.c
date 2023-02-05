#include <stdio.h>
#include <fcntl.h>
#include <errno.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <signal.h>
#include <limits.h>
#include <libgen.h>
#include <stdbool.h>
#include <sys/wait.h>
#include <sys/types.h>

#include "../io/io.h"

#include "promoter.h"

PPROMOTER createPromoter( unsigned int id, char* path )
{
	if ( !path || strlen( path ) > PATH_MAX )
		return NULL;
	
	// allocate promoter structure
	PPROMOTER ppromoter = calloc( 1, sizeof( PROMOTER ) );
	if ( !ppromoter )
		return NULL;
	
	strcpy( ppromoter->path, path );
	
	// create promoter pipe with O_NONBLOCK
	if ( pipe( ppromoter->pipe ) == -1 ||
	     fcntl( ppromoter->pipe[ 0 ], F_SETFL, fcntl( ppromoter->pipe[ 0 ], F_GETFL ) | O_NONBLOCK ) == -1 )
	{
		free( ppromoter );
		return NULL;
	}
	
	// initialize values
	ppromoter->id = id;
	ppromoter->pnext = NULL;
	
	return ppromoter;
}

void deletePromoters( PPROMOTER ppromoter )
{
	if ( !ppromoter )
		return;
	
	// delete all promoters on linked list
	while ( ppromoter )
		ppromoter = deletePromoter( ppromoter );
}

PPROMOTER deletePromoter( PPROMOTER ppromoter )
{
	if ( !ppromoter )
		return NULL;
	
	// delete promoter process
	if ( isPromoterRunning( ppromoter ) )
	{
		kill( ppromoter->process_id, SIGUSR1 );
		waitpid( ppromoter->process_id, NULL, 0 );
	}
	
	if ( ppromoter->pipe[ 0 ] )
		close( ppromoter->pipe[ 0 ] );
	
	if ( ppromoter->pipe[ 1 ] )
		close( ppromoter->pipe[ 1 ] );
	
	// delete promoter and return the next element
	PPROMOTER pnext = ppromoter->pnext;
	memset( ppromoter, 0, sizeof( PROMOTER ) );
	free( ppromoter );
	return pnext;
}

PPROMOTER getLastPromoter( PPROMOTER ppromoter )
{
	if ( !ppromoter )
		return NULL;
	
	// get last element from linked list
	while ( ppromoter->pnext )
		ppromoter = ppromoter->pnext;
	
	return ppromoter;
}

bool startPromoter( PPROMOTER ppromoter )
{
	if ( !ppromoter )
		return false;
	
	if ( isPromoterRunning( ppromoter ) )
		return true;
	
	// create exec
	int process_id = fork( );
	switch ( process_id )
	{
		case 0:
			// close reading end (we are not reading to the pipe only writing to it)
			close( ppromoter->pipe[ 0 ] );
			
			// duplicate the writing end to STDOUT_FILENO closing the original STDOUT_FILENO first
			dup2( ppromoter->pipe[ 1 ], STDOUT_FILENO );
			
			// close the duped file descriptor
			close( ppromoter->pipe[ 1 ] );
			
			// execute promoter (arg1 - time divisor)
			execlp( ppromoter->path, basename( ppromoter->path ), NULL );
		
		case -1:                                                        // in case an error occurs on fork or execl
			perror( "Error Starting promoter" );
			return false;
		
		default:
			// close writing end (we are not writing to the pipe only reading from it)
			close( ppromoter->pipe[ 1 ] );
			
			ppromoter->process_id = process_id;
			break;
	}
	
	return isPromoterRunning( ppromoter );
}

bool stopPromoter( PPROMOTER ppromoter )
{
	if ( !ppromoter || !isPromoterRunning( ppromoter ) )
		return false;
	
	if ( kill( ppromoter->process_id, SIGUSR1 ) == -1 || waitpid( ppromoter->process_id, NULL, 0 ) == -1 )
		return false;
	
	ppromoter->process_id = 0;
	
	// close promoter pipe
	if ( ppromoter->pipe[ 0 ] )
		close( ppromoter->pipe[ 0 ] );
	
	// recreating promoter pipe
	return ( pipe( ppromoter->pipe ) == -1 ||
	         fcntl( ppromoter->pipe[ 0 ], F_SETFL, fcntl( ppromoter->pipe[ 0 ], F_GETFL ) | O_NONBLOCK ) == -1 );
}

PPROMOTER loadPromoters( char* path, char* promoters_dir )
{
	if ( !path || !promoters_dir )
		return NULL;
	
	// load promoters
	char* fbuffer;
	int read_bytes = readFile( path, &fbuffer );
	if ( read_bytes == -1 )
		return NULL;
	
	PPROMOTER ppromoters = NULL, plast = NULL;
	unsigned int id = 1;
	for ( char* line = strtok( fbuffer, "\n" ); line; line = strtok( NULL, "\n" ) )
	{
		char promoters_path[PATH_MAX];
		
		strcpy( promoters_path, promoters_dir );
		strcat( promoters_path, line );
		
		// check if file is valid executable
		if ( !isValidFile( promoters_path, X_OK ) )
		{
			perror( "Invalid Promoter path provided" );
			continue;
		}
		
		// create promoter
		PPROMOTER ppromoter = createPromoter( id, promoters_path );
		if ( !ppromoter )
		{
			// free all promoters memory
			deletePromoters( ppromoters );
			free( fbuffer );
			return NULL;
		}
		
		// insert promoter in linked list
		if ( !ppromoters )
			ppromoters = ppromoter;
		else
			plast->pnext = ppromoter;
		
		plast = ppromoter;
		id++;
	}
	
	free( fbuffer );
	
	return ppromoters;
}

bool reloadPromoters( PPROMOTER* pppromoters, char* path, char* promoters_dir )
{
	if ( !pppromoters || !*pppromoters || !path || !promoters_dir )
		return false;
	
	// load promoters
	char* fbuffer;
	int read_bytes = readFile( path, &fbuffer );
	if ( read_bytes == -1 )
		return false;
	
	PPROMOTER pnew_promoters = NULL, plast = NULL;
	for ( char* line = strtok( fbuffer, "\n" ); line; line = strtok( NULL, "\n" ) )
	{
		PPROMOTER pnew = NULL;
		
		char promoters_path[PATH_MAX];
		
		strcpy( promoters_path, promoters_dir );
		strcat( promoters_path, line );
		
		// check if promoter already exists
		for ( PPROMOTER ppromoter = *pppromoters, ploop_last = NULL;
		      ppromoter;
		      ploop_last = ppromoter, ppromoter = ppromoter->pnext )
			// if it does than we copy it and remove it from old linked list
			if ( !strcmp( promoters_path, ppromoter->path ) )
			{
				pnew = calloc( 1, sizeof( PROMOTER ) );
				if ( !pnew )
				{
					// free all promoters memory
					deletePromoters( pnew_promoters );
					free( fbuffer );
					return false;
				}
				
				memcpy( pnew, ppromoter, sizeof( PROMOTER ) );
				memcpy( pnew->pipe, ppromoter->pipe, sizeof( int ) * 2 );
				pnew->pnext = NULL;
				
				// remove promoter from old linked list
				if ( !ploop_last )
					*pppromoters = ppromoter->pnext;
				else
					ploop_last->pnext = ppromoter->pnext;
				
				free( ppromoter );
				break;
			}
		
		// in case the promoter doesn't exist we create it
		if ( !pnew )
		{
			// check if file is valid executable
			if ( !isValidFile( promoters_path, X_OK ) )
			{
				perror( "Invalid Promoter path provided" );
				continue;
			}
			
			// create promoter
			pnew = createPromoter( 0, promoters_path );
			if ( !pnew )
			{
				// free all promoters memory
				deletePromoters( pnew_promoters );
				free( fbuffer );
				return false;
			}
		}
		
		// insert promoter in linked list
		if ( !pnew_promoters )
			pnew_promoters = pnew;
		else
			plast->pnext = pnew;
		
		plast = pnew;
	}
	
	free( fbuffer );
	
	// assign id's
	for ( PPROMOTER ppromoter = pnew_promoters; ppromoter; ppromoter = ppromoter->pnext )
	{
		// preserve running ids
		if ( isPromoterRunning( ppromoter ) )
			continue;
		
		ppromoter->id = getMinAvPromoterId( pnew_promoters );
		if ( !startPromoter( ppromoter ) )
			printf( "Error running promoter: %s\n", ppromoter->path );
	}
	
	deletePromoters( *pppromoters );
	*pppromoters = pnew_promoters;
	
	return true;
}

bool isPromoterRunning( PPROMOTER ppromoter )
{
	if ( !ppromoter || !ppromoter->process_id )
		return false;
	
	return waitpid( ppromoter->process_id, 0, WNOHANG ) == 0;
}

int readPromoter( PPROMOTER ppromoter, char** pbuffer )
{
	if ( !ppromoter || !ppromoter->process_id )
		return -1;
	
	if ( !isPromoterRunning( ppromoter ) )
	{
		printf( "Promoter Died" );
		exit( 1 );
	}
	
	char buffer[250];
	int num_bytes = ( int ) read( ppromoter->pipe[ 0 ], buffer, 250 );
	if ( num_bytes <= 0 )
	{
		// ignore would block operations
		if ( errno == EWOULDBLOCK )
			return 0;
		
		return num_bytes;
	}
	
	// delete '\n'
	buffer[ num_bytes - 1 ] = '\0';
	
	int size = ( int ) strlen( buffer ) + 1;
	
	*pbuffer = calloc( 1, size );
	if ( !( *pbuffer ) )
		return -1;
	
	memcpy( *pbuffer, buffer, size );
	
	return size;
}

char* getPromoterFileName( PPROMOTER ppromoter )
{
	if ( !ppromoter )
		return NULL;
	
	char* ret = basename( ppromoter->path );
	if ( !ret )
		return NULL;
	
	return strdup( ret );
}

unsigned int getMinAvPromoterId( PPROMOTER ppromoters )
{
	if ( !ppromoters )
		return 1;
	
	unsigned int min = 1;
	for ( PPROMOTER ppromoter = ppromoters; ppromoter; ppromoter = ppromoter->pnext )
		if ( min == ppromoter->id )
		{
			min++;
			ppromoter = ppromoters;
			
			if ( ppromoter->id == min )
				min++;
			
			continue;
		}
	
	return min;
}