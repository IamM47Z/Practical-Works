#include <stdio.h>
#include <ctype.h>
#include <stdlib.h>
#include <string.h>
#include <signal.h>
#include <unistd.h>

#include "io/io.h"
#include "backend/backend.h"
#include "insparse/insparse.h"

PBACKEND pbackend = NULL;

void exitHandler( )
{
	if ( pbackend )
	{
		deleteBackend( pbackend );
		pbackend = NULL;
	}
}

void signalHandler( int sig )
{
	exitHandler( );
	
	printf( "\n\nSignal received: %s", strsignal( sig ) );
	
	// exit with sig code
	exit( 128 + sig );
}

int main( int argc, char** argv )
{
	// register our handlers
	//
	atexit( exitHandler );
	
	signal( SIGINT, signalHandler );
	signal( SIGHUP, signalHandler );
	signal( SIGQUIT, signalHandler );
	signal( SIGTERM, signalHandler );
	signal( SIGABRT, signalHandler );
	signal( SIGPIPE, signalHandler );
	
	printf( "SOBay - BackEnd\n\n" );
	
	if ( !( pbackend = createBackend( ) ) )
		return 1;
	
	if ( !startBackendPromoters( pbackend ) )
		return 1;
	
	// loop both fd's
	//
	printf( "Command: " );
	fflush( stdout );
	
	fd_set read_fds;
	char ins_buffer[200];
	INS_INFO ins_info = { 0 };
	while ( ins_info.type != CLOSE )
	{
		// initialize set
		//
		FD_ZERO( &read_fds );
		FD_SET( STDIN_FILENO, &read_fds );
		FD_SET( pbackend->announcements_pipe[ 0 ], &read_fds );
		
		int nfds = select( pbackend->announcements_pipe[ 0 ] + 1, &read_fds, NULL, NULL, NULL );
		if ( nfds == -1 )
		{
			perror( "Error using Select" );
			exit( 1 );
		}
		
		if ( FD_ISSET( STDIN_FILENO, &read_fds ) )
		{
			if ( fgets( ins_buffer, sizeof( ins_buffer ), stdin ) <= 0 )
				continue;
			
			// remove newline char
			if ( ins_buffer[ strlen( ins_buffer ) - 1 ] == '\n' )
				ins_buffer[ strlen( ins_buffer ) - 1 ] = '\0';
			
			// parsing instruction
			if ( !parseInstruction( ins_buffer, sizeof( ins_buffer ), &ins_info ) )
			{
				printf( "Command: " );
				fflush( stdout );
				fflush( stdin );
				continue;
			}
			
			printf( "\n" );
			
			// executing instruction
			switch ( ins_info.type )
			{
				case USERS:
				{
					if ( pthread_mutex_lock( &pbackend->clients_mutex ) )
					{
						perror( "Error locking Clients mutex" );
						exit( 1 );
					}
					
					printf( "Connected Users:\n" );
					if ( pbackend->num_online_clients )
						for ( int i = 0; i < pbackend->num_online_clients; i++ )
						{
							PCLIENT pclient = &pbackend->pclients[ i ];
							printf( "\t%s [ %d ]\n", pclient->username, pclient->pid );
						}
					else
						printf( "\tNone\n" );
					
					printf( "\n" );
					pthread_mutex_unlock( &pbackend->clients_mutex );
					break;
				}
				case LIST:
				{
					if ( pthread_mutex_lock( &pbackend->auctions_mutex ) )
					{
						perror( "Error obtaining Auctions Lock" );
						exit( 1 );
					}
					
					printf( "Current Auctions:\n" );
					
					for ( PAUCTION pauction = pbackend->pauctions; pauction; pauction = pauction->pnext )
						printf( "\t[ %d ] %s is selling %s for %d€\n", pauction->item_id, pauction->seller_name,
						        pauction->name,
						        getAuctionFinalPrice( pauction ) );
					
					pthread_mutex_unlock( &pbackend->auctions_mutex );
					break;
				}
				case KICK:
				{
					if ( pthread_mutex_lock( &pbackend->clients_mutex ) )
					{
						perror( "Error locking Clients mutex" );
						exit( 1 );
					}
					
					PCLIENT pclient = getClientByUsername( pbackend->pclients, pbackend->num_online_clients,
					                                       ins_info.buffer );
					if ( !pclient )
					{
						printf( "User %s not found\n", ins_info.buffer );
						pthread_mutex_unlock( &pbackend->clients_mutex );
						break;
					}
					
					REQUEST req;
					memset( &req, 0, sizeof( REQUEST ) );
					req.from = 0;
					req.to = pclient->pid;
					req.type = REQ_DISCONNECT;
					if ( writeIoFifo( &pclient->io, ( char* ) &req, sizeof( req ) ) == -1 )
					{
						perror( "Error writing to Client Fifo" );
						pthread_mutex_unlock( &pbackend->clients_mutex );
						break;
					}
					
					if ( !removeClient( pbackend->pclients, pbackend->num_online_clients, pclient->pid ) )
					{
						perror( "Error removing client" );
						pthread_mutex_unlock( &pbackend->clients_mutex );
						break;
					}
					
					pbackend->num_online_clients--;
					
					pthread_mutex_unlock( &pbackend->clients_mutex );
					
					printf( "User %s Kicked Successfully\n", ins_info.buffer );
					break;
				}
				case PROM:
				{
					if ( pthread_mutex_lock( &pbackend->promoters_mutex ) )
					{
						perror( "Error obtaining Promoters Lock" );
						exit( 1 );
					}
					
					printf( "Current Promoters:\n" );
					
					for ( PPROMOTER ppromoter = pbackend->ppromoters; ppromoter; ppromoter = ppromoter->pnext )
					{
						if ( isPromoterRunning( ppromoter ) )
							printf( "\t[ %d ] Promoter: \"%s\" Routine executing at: %d\n", ppromoter->id,
							        ppromoter->path,
							        ppromoter->process_id );
						else
							printf( "\t[ %d ] Promoter: \"%s\" Routine not being executed\n", ppromoter->id,
							        ppromoter->path );
					}
					
					pthread_mutex_unlock( &pbackend->promoters_mutex );
					break;
				}
				case REPROM:
				{
					char* promoters_fname = getenv( "FPROMOTERS" );
					if ( !promoters_fname )
					{
						printf( "There is no FPROMOTERS global env!\n" );
						break;
					}
					
					if ( pthread_mutex_lock( &pbackend->promoters_mutex ) )
					{
						printf( "Error locking promoters mutex" );
						exit( 1 );
					}
					
					char buffer[PATH_MAX];
					strcpy( buffer, pbackend->cur_executable_path );
					strcat( buffer, "/files/" );
					strcat( buffer, promoters_fname );
					
					char promoters_dir[PATH_MAX];
					strcpy( promoters_dir, pbackend->cur_executable_path );
					strcat( promoters_dir, "/promoters/" );
					if ( isValidFile( buffer, R_OK ) &&
					     reloadPromoters( &pbackend->ppromoters, buffer, promoters_dir ) )
					{
						printf( "Promoters reloaded successfully!\n" );
						pthread_mutex_unlock( &pbackend->promoters_mutex );
						break;
					}
					
					perror( "Fatal error reloading promoters" );
					pthread_mutex_unlock( &pbackend->promoters_mutex );
					exit( 1 );
				}
				case CANCEL:
				{
					if ( pthread_mutex_lock( &pbackend->promoters_mutex ) )
					{
						printf( "Error locking promoters mutex" );
						exit( 1 );
					}
					
					bool found = false;
					for ( PPROMOTER ppromoter = pbackend->ppromoters, plast = NULL;
					      ppromoter; )
					{
						char* temp_name = getPromoterFileName( ppromoter );
						if ( temp_name && !strcmp( ins_info.buffer, temp_name ) )
						{
							found = true;
							free( temp_name );
							
							printf( "Promoter [ %d ] \"%s\" has been canceled!\n", ppromoter->id, ins_info.buffer );
							
							// removing element
							//
							ppromoter = deletePromoter( ppromoter );
							
							if ( plast )
								plast->pnext = ppromoter;
							else
								pbackend->ppromoters = ppromoter;
							
							continue;
						}
						
						free( temp_name );
						
						plast = ppromoter;
						ppromoter = ppromoter->pnext;
					}
					
					if ( !found )
						printf( "Promoter named \"%s\" not found!\n", ins_info.buffer );
					
					pthread_mutex_unlock( &pbackend->promoters_mutex );
					break;
				}
				case CLOSE:
				{
					printf( "Terminating clients and closing backend.\n" );
					exit( 0 );
				}
				case HELP:
				{
					for ( int i = 0; i < HELP + 1; i++ )
						printf( "%s", help_messages[ i ] );
					break;
				}
				default:
					break;
			}
			
			printf( "\nCommand: " );
			fflush( stdout );
			fflush( stdin );
		}
		
		if ( FD_ISSET( pbackend->announcements_pipe[ 0 ], &read_fds ) )
		{
			char buffer[256];
			ssize_t read_ret;
			while ( ( read_ret = read( pbackend->announcements_pipe[ 0 ], buffer, sizeof( buffer ) ) ) > 0 )
			{
				if ( read_ret == -1 )
				{
					perror( "Error reading Announcements pipe" );
					exit( 1 );
				}
				
				printf( "%s", buffer );
			}
			
			printf( "Command: " );
			fflush( stdout );
			fflush( stdin );
		}
	}
	
	return 0;
}
