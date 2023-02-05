#include <stdio.h>
#include <errno.h>
#include <stdlib.h>
#include <libgen.h>
#include <limits.h>
#include <sys/stat.h>

#ifdef __APPLE__

#include <mach-o/dyld.h>

#endif

#include "../io/io.h"
#include "../users/users_lib.h"

#include "backend.h"

// processTime
// this thread will handle the time counter and other time related functions
//
void processTime( PBACKEND pbackend )
{
	while ( pbackend )
	{
		pbackend->uptime++;
		
		pthread_setcancelstate( PTHREAD_CANCEL_DISABLE, NULL );
		
		if ( processBackendPromoters( pbackend ) == -1 )
		{
			perror( "Error processing Promoters" );
			exit( 1 );
		}
		
		if ( processBackendPromotions( pbackend ) == -1 )
		{
			perror( "Error processing Promotions" );
			exit( 1 );
		}
		
		if ( processBackendAuctions( pbackend ) == -1 )
		{
			perror( "Error processing Auctions" );
			exit( 1 );
		}
		
		if ( processBackendClients( pbackend ) == -1 )
		{
			perror( "Error processing Clients" );
			exit( 1 );
		}
		
		pthread_setcancelstate( PTHREAD_CANCEL_ENABLE, NULL );
		
		sleep( 1 );                                             // would be better to implement timer_t but no time for that
	}
	
	perror( "Error Processing Time" );
	exit( 1 );
}

// processRequests
// this thread will handle all client requests
//
void processRequests( PBACKEND pbackend )
{
	while ( pbackend )
	{
		// sleep for 1 microsecond to ensure the thread can be canceled
		//
		usleep( 1 );
		
		pthread_setcancelstate( PTHREAD_CANCEL_DISABLE, NULL );
		
		REQUEST req;
		memset( &req, 0, sizeof( REQUEST ) );
		int num_bytes = readIoFifo( &pbackend->io, ( char* ) &req, sizeof( req ) );
		if ( num_bytes <= 0 )
		{
			if ( !num_bytes || errno == EWOULDBLOCK )
			{
				pthread_setcancelstate( PTHREAD_CANCEL_ENABLE, NULL );
				
				continue;
			}
			
			perror( "Error reading IO Fifo" );
			exit( 1 );
		}
		
		if ( req.to == 0 )
		{
			REQUEST send;
			memset( &send, 0, sizeof( REQUEST ) );
			
			send.from = 0;
			send.to = req.from;
			send.type = req.type;
			
			bool needs_answer = true;
			
			if ( pthread_mutex_lock( &pbackend->clients_mutex ) )
			{
				perror( "Error locking Clients mutex" );
				exit( 1 );
			}
			
			PCLIENT pclient = getClientByPid( pbackend->pclients, pbackend->num_online_clients, req.from );
			if ( pclient )
				switch ( req.type )
				{
					case REQ_SELL:
					{
						if ( pthread_mutex_lock( &pbackend->auctions_mutex ) )
						{
							perror( "Error locking auctions mutex" );
							pthread_mutex_unlock( &pbackend->clients_mutex );
							exit( 1 );
						}
						
						unsigned int id = getMaxAuctionId( pbackend->pauctions ) + 1;
						PAUCTION pauction = createAuction( id, req.response.front.sell.name,
						                                   req.response.front.sell.category,
						                                   pclient->username, req.response.front.sell.price,
						                                   req.response.front.sell.buy_now_price,
						                                   req.response.front.sell.duration );
						
						send.response.status = pauction != NULL;
						
						if ( pauction )
						{
							if ( pbackend->pauctions )
								getLastAuction( pbackend->pauctions )->pnext = pauction;
							else
								pbackend->pauctions = pauction;
							
							REQUEST announce_req;
							memset( &announce_req, 0, sizeof( REQUEST ) );
							announce_req.to = -1;
							announce_req.from = 0;
							announce_req.type = REQ_PRINT;
							sprintf( announce_req.response.back.buffer,
							         "Item %s (%s) with an ID of %d is being auctioned off by %s. The starting price is %d$, the Buy Now price is %d$, and the auction will end in %d scs.",
							         pauction->name, pauction->category, pauction->item_id, pauction->seller_name,
							         pauction->current_bid.value, pauction->buy_now_value, pauction->timer );
							
							for ( int i = 0; i < pbackend->num_online_clients; i++ )
							{
								PIO pio = &pbackend->pclients[ i ].io;
								if ( pio->initialized &&
								     strcmp( pbackend->pclients[ i ].username, pauction->seller_name ) != 0 )
									writeIoFifo( pio, ( char* ) &announce_req, sizeof( announce_req ) );
							}
							
							char announcement_buffer[276];
							sprintf( announcement_buffer, "\r\r[ Announcement ] %s\n",
							         announce_req.response.back.buffer );
							write( pbackend->announcements_pipe[ 1 ], announcement_buffer,
							       strlen( announcement_buffer ) + 1 );
						}
						
						pthread_mutex_unlock( &pbackend->auctions_mutex );
						break;
					}
					case REQ_LIST:
					{
						if ( pthread_mutex_lock( &pbackend->auctions_mutex ) )
						{
							perror( "Error locking auctions mutex" );
							pthread_mutex_unlock( &pbackend->clients_mutex );
							exit( 1 );
						}
						
						send.response.back.list.num = 0;
						
						for ( PAUCTION pauction = pbackend->pauctions;
						      pauction;
						      pauction = pauction->pnext )
						{
							if ( isAuctionFinished( pauction ) )
								continue;
							
							// filters
							//
							if ( req.response.front.list.by_cat &&
							     strcmp( req.response.front.list.cat, pauction->category ) != 0 )
								continue;
							
							if ( req.response.front.list.by_user &&
							     strcmp( req.response.front.list.user, pauction->seller_name ) != 0 )
								continue;
							
							if ( req.response.front.list.by_price &&
							     req.response.front.list.price < pauction->current_bid.value )
								continue;
							
							if ( req.response.front.list.by_time &&
							     req.response.front.list.time < pauction->timer )
								continue;
							
							memcpy( &send.response.back.list.auctions[ send.response.back.list.num++ ],
							        pauction,
							        sizeof( AUCTION ) );
						}
						
						send.response.status = true;
						pthread_mutex_unlock( &pbackend->auctions_mutex );
						break;
					}
					case REQ_TIME:
					{
						send.response.status = true;
						send.response.back.uptime = pbackend->uptime;
						break;
					}
					case REQ_BID:
					{
						if ( pthread_mutex_lock( &pbackend->auctions_mutex ) )
						{
							perror( "Error locking Auctions mutex" );
							pthread_mutex_unlock( &pbackend->clients_mutex );
							exit( 1 );
						}
						
						PAUCTION pauction = getAuctionById( pbackend->pauctions, req.response.front.bid.auction_id );
						if ( !pauction )
						{
							send.response.status = false;
							pthread_mutex_unlock( &pbackend->auctions_mutex );
							break;
						}
						
						if ( req.response.front.bid.value >= pauction->buy_now_value )
						{
							send.response.status = buyNowBackendAuction( pbackend, pauction, pclient,
							                                             req.response.front.bid.value );
							send.response.back.purchased = send.response.status;
						}
						else
							send.response.status = bidBackendAuction( pbackend, pauction, pclient,
							                                          req.response.front.bid.value );
						
						pthread_mutex_unlock( &pbackend->auctions_mutex );
						break;
					}
					case REQ_BALANCE:
					{
						int balance = getUserBalance( pclient->username );
						send.response.status = balance != -1;
						if ( balance != -1 )
							send.response.back.balance = balance;
						break;
					}
					case REQ_ADD:
					{
						int balance = getUserBalance( pclient->username );
						if ( balance == -1 )
						{
							send.response.status = false;
							break;
						}
						
						balance += ( int ) req.response.front.add.value;
						
						int ret = updateUserBalance( pclient->username, balance );
						if ( ret == -1 )
						{
							perror( "Error using Users library" );
							pthread_mutex_unlock( &pbackend->clients_mutex );
							exit( 1 );
						}
						
						send.response.status = ret == 1;
						break;
					}
					case REQ_HEARTBEAT:
					{
						updateClientHeartbeat( pbackend, req.from );
						needs_answer = false;
						break;
					}
					case REQ_DISCONNECT:
					{
						needs_answer = false;
						
						if ( !removeClient( pbackend->pclients, pbackend->num_online_clients, pclient->pid ) )
						{
							perror( "Error Removing Client" );
							pthread_mutex_unlock( &pbackend->clients_mutex );
							exit( 1 );
						}
						
						pbackend->num_online_clients--;
						break;
					}
					default:
						needs_answer = false;
						break;
				}
			else
			{
				if ( req.type == REQ_LOGIN )
				{
					send.response.status = addClient( pbackend->pclients, pbackend->num_online_clients,
					                                  req.response.front.login.pathname,
					                                  req.response.front.login.username,
					                                  req.response.front.login.password, req.from );
					
					if ( send.response.status )
					{
						pbackend->num_online_clients++;
						pclient = getClientByPid( pbackend->pclients, pbackend->num_online_clients, req.from );
					}
					else
					{
						needs_answer = false;
						
						IO temp_io;
						if ( !initializeIo( &temp_io, req.response.front.login.pathname, O_WRONLY ) )
							perror( "Error trying to answer a failed login!" );
						else
						{
							if ( writeIoFifo( &temp_io, ( char* ) &send, sizeof( send ) ) == -1 )
							{
								perror( "Error writing IO Fifo" );
								pthread_mutex_unlock( &pbackend->clients_mutex );
								exit( 1 );
							}
							
							destroyIo( &temp_io );
							printf( "Denied login to %d\n", req.from );
						}
					}
				}
				else
					needs_answer = false;
			}
			
			pthread_mutex_unlock( &pbackend->clients_mutex );
			
			if ( needs_answer && pclient->io.initialized &&
			     writeIoFifo( &pclient->io, ( char* ) &send, sizeof( send ) ) == -1 )
			{
				perror( "Error writing IO Fifo" );
				exit( 1 );
			}
		}
		
		pthread_setcancelstate( PTHREAD_CANCEL_ENABLE, NULL );
	}
	
	perror( "Error Processing Clients" );
	exit( 1 );
}

//
// Backend Related Functions
//

PBACKEND createBackend( )
{
	int fifo_fd = open( "AuctionServer", O_RDWR );
	if ( fifo_fd > -1 )
	{
		close( fifo_fd );
		printf( "You can Only open a Backend at a time." );
		exit( 1 );
	}
	
	char* items_fname = getenv( "FITEMS" );
	char* users_fname = getenv( "FUSERS" );
	char* str_heartbeat = getenv( "HEARTBEAT" );
	char* promoters_fname = getenv( "FPROMOTERS" );
	if ( !items_fname || !users_fname || !promoters_fname || !str_heartbeat )
		return NULL;
	
	PBACKEND pbackend = calloc( 1, sizeof( BACKEND ) );
	if ( !pbackend )
		return NULL;
	
	int heartbeat = ( int ) strtol( str_heartbeat, NULL, 10 );
	if ( heartbeat <= 0 )
	{
		perror( "Invalid HeartBeat value" );
		free( pbackend );
		return NULL;
	}
	
	pbackend->heartbeat = heartbeat;
	
	// get executable directory path
	char full_path[PATH_MAX];
#ifdef __APPLE__
	uint32_t size = PATH_MAX;
	if ( _NSGetExecutablePath( full_path, &size ) == 0 )
#else
		int bytes = readlink( "/proc/self/exe", full_path, PATH_MAX );
		if ( bytes > 0 )
#endif
		strcpy( pbackend->cur_executable_path, dirname( full_path ) );
	else
	{
		perror( "Error obtaining executable path!" );
		free( pbackend );
		return NULL;
	}
	
	if ( pthread_mutex_init( &pbackend->clients_mutex, NULL ) )
	{
		free( pbackend );
		return NULL;
	}
	
	if ( pthread_mutex_init( &pbackend->auctions_mutex, NULL ) )
	{
		pthread_mutex_destroy( &pbackend->clients_mutex );
		free( pbackend );
		return NULL;
	}
	
	if ( pthread_mutex_init( &pbackend->promoters_mutex, NULL ) )
	{
		pthread_mutex_destroy( &pbackend->auctions_mutex );
		pthread_mutex_destroy( &pbackend->clients_mutex );
		free( pbackend );
		return NULL;
	}
	
	if ( pthread_mutex_init( &pbackend->promotions_mutex, NULL ) )
	{
		pthread_mutex_destroy( &pbackend->promoters_mutex );
		pthread_mutex_destroy( &pbackend->auctions_mutex );
		pthread_mutex_destroy( &pbackend->clients_mutex );
		free( pbackend );
		return NULL;
	}
	
	pbackend->ppromotions = NULL;
	
	char buffer[PATH_MAX];
	strcpy( buffer, pbackend->cur_executable_path );
	strcat( buffer, "/files/" );
	strcat( buffer, items_fname );
	pbackend->pauctions = isValidFile( buffer, R_OK ) ?
	                      loadAuctions( buffer ) : NULL;
	
	strcpy( buffer, pbackend->cur_executable_path );
	strcat( buffer, "/files/" );
	strcat( buffer, users_fname );
	pbackend->num_users = isValidFile( buffer, R_OK ) ?
	                      loadUsersFile( buffer ) : 0;
	
	strcpy( buffer, pbackend->cur_executable_path );
	strcat( buffer, "/files/" );
	strcat( buffer, promoters_fname );
	
	char promoters_dir[PATH_MAX];
	strcpy( promoters_dir, pbackend->cur_executable_path );
	strcat( promoters_dir, "/promoters/" );
	pbackend->ppromoters = isValidFile( buffer, R_OK ) ?
	                       loadPromoters( buffer, promoters_dir ) : NULL;
	
	pbackend->uptime = 0;
	pbackend->num_online_clients = 0;
	memset( pbackend->pclients, 0, sizeof( CLIENT ) * 20 );
	
	if ( mkfifo( "AuctionServer", 0666 ) == -1 )
	{
		pthread_mutex_destroy( &pbackend->promotions_mutex );
		pthread_mutex_destroy( &pbackend->promoters_mutex );
		pthread_mutex_destroy( &pbackend->auctions_mutex );
		pthread_mutex_destroy( &pbackend->clients_mutex );
		free( pbackend );
		return NULL;
	}
	
	if ( !initializeIo( &pbackend->io, "AuctionServer", O_RDONLY | O_NONBLOCK ) )
	{
		unlink( "AuctionServer" );
		
		pthread_mutex_destroy( &pbackend->promotions_mutex );
		pthread_mutex_destroy( &pbackend->promoters_mutex );
		pthread_mutex_destroy( &pbackend->auctions_mutex );
		pthread_mutex_destroy( &pbackend->clients_mutex );
		free( pbackend );
		return NULL;
	}
	
	if ( pthread_create( &pbackend->time_thread, NULL, ( void* ( * )( void* ) ) processTime, pbackend ) )
	{
		unlink( "AuctionServer" );
		
		destroyIo( &pbackend->io );
		
		pthread_mutex_destroy( &pbackend->promotions_mutex );
		pthread_mutex_destroy( &pbackend->promoters_mutex );
		pthread_mutex_destroy( &pbackend->auctions_mutex );
		pthread_mutex_destroy( &pbackend->clients_mutex );
		free( pbackend );
		return NULL;
	}
	
	if ( pthread_create( &pbackend->worker_thread, NULL, ( void* ( * )( void* ) ) processRequests, pbackend ) )
	{
		unlink( "AuctionServer" );
		
		if ( !pthread_cancel( pbackend->time_thread ) )
			pthread_join( pbackend->time_thread, NULL );
		
		destroyIo( &pbackend->io );
		
		pthread_mutex_destroy( &pbackend->promotions_mutex );
		pthread_mutex_destroy( &pbackend->promoters_mutex );
		pthread_mutex_destroy( &pbackend->auctions_mutex );
		pthread_mutex_destroy( &pbackend->clients_mutex );
		free( pbackend );
		return NULL;
	}
	
	if ( pipe( pbackend->announcements_pipe ) == -1 ||
	     fcntl( pbackend->announcements_pipe[ 0 ], F_SETFL,
	            fcntl( pbackend->announcements_pipe[ 0 ], F_GETFL ) | O_NONBLOCK ) )
	{
		unlink( "AuctionServer" );
		
		if ( !pthread_cancel( pbackend->worker_thread ) )
			pthread_join( pbackend->time_thread, NULL );
		
		if ( !pthread_cancel( pbackend->time_thread ) )
			pthread_join( pbackend->time_thread, NULL );
		
		destroyIo( &pbackend->io );
		
		pthread_mutex_destroy( &pbackend->promotions_mutex );
		pthread_mutex_destroy( &pbackend->promoters_mutex );
		pthread_mutex_destroy( &pbackend->auctions_mutex );
		pthread_mutex_destroy( &pbackend->clients_mutex );
		free( pbackend );
		return NULL;
	}
	
	return pbackend;
}

void deleteBackend( PBACKEND pbackend )
{
	if ( !pbackend )
		return;
	
	if ( pbackend->time_thread )
		if ( !pthread_cancel( pbackend->time_thread ) )
			pthread_join( pbackend->time_thread, NULL );
	
	if ( pbackend->worker_thread )
		if ( !pthread_cancel( pbackend->worker_thread ) )
			pthread_join( pbackend->worker_thread, NULL );
	
	REQUEST req;
	memset( &req, 0, sizeof( REQUEST ) );
	req.to = -1;
	req.from = 0;
	req.type = REQ_DISCONNECT;
	
	for ( int i = 0; i < pbackend->num_online_clients; i++ )
	{
		PIO pio = &pbackend->pclients[ i ].io;
		if ( pio->initialized )
		{
			if ( writeIoFifo( pio, ( char* ) &req, sizeof( req ) ) == -1 )
				printf( "Error writing disconnect request to %d. Error: %s\n", pbackend->pclients[ i ].pid,
				        strerror( errno ) );
			
			destroyIo( pio );
		}
	}
	
	if ( strlen( pbackend->cur_executable_path ) )
	{
		char* users_fname = getenv( "FUSERS" );
		char* items_fname = getenv( "FITEMS" );
		
		char buffer[PATH_MAX];
		strcpy( buffer, pbackend->cur_executable_path );
		strcat( buffer, "/files/" );
		strcat( buffer, users_fname );
		if ( users_fname && isValidFile( users_fname, W_OK ) && saveUsersFile( buffer ) )
			perror( "Error saving users file" );
		
		strcpy( buffer, pbackend->cur_executable_path );
		strcat( buffer, "/files/" );
		strcat( buffer, items_fname );
		if ( items_fname && !saveAuctions( pbackend->pauctions, buffer ) )
			perror( "Error saving items file" );
	}
	
	if ( pbackend->ppromoters )
		deletePromoters( pbackend->ppromoters );
	
	if ( pbackend->ppromotions )
		deletePromotions( pbackend->ppromotions );
	
	if ( pbackend->pauctions )
		deleteAuctions( pbackend->pauctions );
	
	pthread_mutex_destroy( &pbackend->clients_mutex );
	pthread_mutex_destroy( &pbackend->auctions_mutex );
	pthread_mutex_destroy( &pbackend->promoters_mutex );
	pthread_mutex_destroy( &pbackend->promotions_mutex );
	
	if ( pbackend->io.initialized )
		destroyIo( &pbackend->io );
	
	unlink( pbackend->io.pathname );
	
	if ( pbackend->announcements_pipe[ 0 ] )
		close( pbackend->announcements_pipe[ 0 ] );
	
	if ( pbackend->announcements_pipe[ 1 ] )
		close( pbackend->announcements_pipe[ 1 ] );
	
	memset( pbackend, 0, sizeof( BACKEND ) );
	free( pbackend );
}

bool startBackendPromoters( PBACKEND pbackend )
{
	if ( !pbackend )
		return false;
	
	if ( !pbackend->ppromoters )
		return true;
	
	if ( pthread_mutex_lock( &pbackend->promoters_mutex ) )
		return false;
	
	PPROMOTER ppromoter = pbackend->ppromoters;
	while ( ppromoter )
	{
		if ( !startPromoter( ppromoter ) )
		{
			pthread_mutex_unlock( &pbackend->promoters_mutex );
			stopBackendPromoters( pbackend );
			return false;
		}
		
		ppromoter = ppromoter->pnext;
	}
	
	pthread_mutex_unlock( &pbackend->promoters_mutex );
	
	return true;
}

int stopBackendPromoters( PBACKEND pbackend )
{
	if ( !pbackend )
		return -1;
	
	if ( !pbackend->ppromoters )
		return 0;
	
	if ( pthread_mutex_lock( &pbackend->promoters_mutex ) )
		return -1;
	
	int num = 0;
	PPROMOTER ppromoter = pbackend->ppromoters;
	while ( ppromoter )
	{
		if ( stopPromoter( ppromoter ) )
			num++;
		
		ppromoter = ppromoter->pnext;
	}
	
	pthread_mutex_unlock( &pbackend->promoters_mutex );
	
	return num;
}

int processBackendPromoters( PBACKEND pbackend )
{
	if ( !pbackend )
		return -1;
	
	if ( !pbackend->ppromoters )
		return 0;
	
	if ( pthread_mutex_lock( &pbackend->promoters_mutex ) )
		return -1;
	
	int ret = 0;
	for ( PPROMOTER ppromoter = pbackend->ppromoters; ppromoter; ppromoter = ppromoter->pnext )
	{
		if ( !isPromoterRunning( ppromoter ) )
			continue;
		
		char* buffer;
		switch ( readPromoter( ppromoter, &buffer ) )
		{
			case 0:
				// no information to read
				continue;
			
			case -1:
				// error
				perror( "Error reading promoter" );
				pthread_mutex_unlock( &pbackend->promoters_mutex );
				return false;
			
			default:
			{
				if ( pthread_mutex_lock( &pbackend->promotions_mutex ) )
				{
					perror( "Error locking promotions mutex" );
					pthread_mutex_unlock( &pbackend->promoters_mutex );
					exit( 1 );
				}
				
				char* line = buffer;
				while ( line )
				{
					char* next_line = strchr( line, '\n' );
					if ( next_line )
						*next_line = '\0';
					
					char* promo_category = strtok( line, " " );
					line = strtok( NULL, " " );
					
					long args[2];
					for ( int i = 0; line; i++ )
					{
						// get all args
						char* end_buffer;
						args[ i ] = strtol( line, &end_buffer, 10 );
						
						line = strtok( NULL, " " );
					}
					
					float promo_value = 1.0f - ( ( float ) args[ 1 ] ) / 100;
					PPROMOTION ppromotion = createPromotion( promo_category, args[ 0 ], promo_value, ppromoter->id );
					if ( !ppromotion )
					{
						perror( "Error creating promotion" );
						pthread_mutex_unlock( &pbackend->promotions_mutex );
						pthread_mutex_unlock( &pbackend->promoters_mutex );
						exit( 1 );
					}
					
					ret++;
					
					if ( !pbackend->ppromotions )
						pbackend->ppromotions = ppromotion;
					else
						getLastPromotion( pbackend->ppromotions )->pnext = ppromotion;
					
					if ( next_line )
						line = next_line + 1;
					else
						line = NULL;
					
					
					REQUEST req;
					memset( &req, 0, sizeof( REQUEST ) );
					req.to = -1;
					req.from = 0;
					req.type = REQ_PRINT;
					sprintf( req.response.back.buffer,
					         "New promotion of %ld%% on %s from Promoter %d. Remaining time: %ld scs.",
					         args[ 1 ], promo_category, ppromoter->id, args[ 0 ] );
					
					for ( int i = 0; i < pbackend->num_online_clients; i++ )
					{
						PIO pio = &pbackend->pclients[ i ].io;
						if ( pio->initialized )
							writeIoFifo( pio, ( char* ) &req, sizeof( req ) );
					}
					
					char announcement_buffer[276];
					sprintf( announcement_buffer, "\r\r[ Announcement ] %s\n", req.response.back.buffer );
					write( pbackend->announcements_pipe[ 1 ], announcement_buffer, strlen( announcement_buffer ) + 1 );
				}
				
				pthread_mutex_unlock( &pbackend->promotions_mutex );
				
				break;
			}
		}
		
		free( buffer );
	}
	
	pthread_mutex_unlock( &pbackend->promoters_mutex );
	
	return ret;
}

int processBackendAuctions( PBACKEND pbackend )
{
	if ( !pbackend )
		return -1;
	
	if ( !pbackend->pauctions )
		return 0;
	
	if ( pthread_mutex_lock( &pbackend->auctions_mutex ) )
		return -1;
	
	int ret = 0;
	for ( PAUCTION pauction = pbackend->pauctions, plast = NULL; pauction; )
	{
		if ( pauction->timer && --pauction->timer )
		{
			plast = pauction;
			pauction = pauction->pnext;
			continue;
		}
		
		// in case the timer is zeroed we increase the return count
		ret++;
		
		REQUEST req;
		memset( &req, 0, sizeof( REQUEST ) );
		req.to = -1;
		req.from = 0;
		req.type = REQ_PRINT;
		
		if ( strcmp( pauction->current_bid.username, "-" ) != 0 )
		{
			sprintf( req.response.back.buffer, "Auction %s from %s has finished! Winner: %s Price: %d$.",
			         pauction->name, pauction->seller_name, pauction->current_bid.username,
			         getAuctionFinalPrice( pauction ) );
			
			// payment
			//
			int balance = getUserBalance( pauction->seller_name );
			if ( balance == -1 )
				printf( "Error accessing User balance: %s\n", getLastErrorText( ) );
			else
			{
				balance += getAuctionFinalPrice( pauction );
				
				if ( updateUserBalance( pauction->seller_name, balance ) == -1 )
					printf( "Error updating User balance: %s\n", getLastErrorText( ) );
			}
		}
		else
			sprintf( req.response.back.buffer, "Auction %s from %s has finished! There was no winner.",
			         pauction->name, pauction->seller_name );
		
		
		for ( int i = 0; i < pbackend->num_online_clients; i++ )
		{
			PIO pio = &pbackend->pclients[ i ].io;
			if ( pio->initialized )
				writeIoFifo( pio, ( char* ) &req, sizeof( req ) );
		}
		
		char announcement_buffer[276];
		sprintf( announcement_buffer, "\r\r[ Announcement ] %s\n", req.response.back.buffer );
		write( pbackend->announcements_pipe[ 1 ], announcement_buffer, strlen( announcement_buffer ) + 1 );
		
		// removing element
		//
		pauction = deleteAuction( pauction );
		
		if ( plast )
			plast->pnext = pauction;
		else
			pbackend->pauctions = pauction;
	}
	
	pthread_mutex_unlock( &pbackend->auctions_mutex );
	
	return ret;
}

int processBackendPromotions( PBACKEND pbackend )
{
	if ( !pbackend )
		return -1;
	
	if ( !pbackend->ppromotions )
		return 0;
	
	if ( pthread_mutex_lock( &pbackend->promotions_mutex ) )
		return -1;
	
	int ret = 0;
	for ( PPROMOTION ppromotion = pbackend->ppromotions, plast = NULL;
	      ppromotion;
	      plast = ppromotion, ppromotion = ppromotion->pnext )
	{
		if ( !ppromotion->timer )
			continue;
		
		ppromotion->timer--;
		if ( ppromotion->timer )
			continue;
		
		REQUEST req;
		memset( &req, 0, sizeof( REQUEST ) );
		req.to = -1;
		req.from = 0;
		req.type = REQ_PRINT;
		sprintf( req.response.back.buffer,
		         "Promotion of %d%% on %s from Promoter %d has finished!",
		         ( int ) ( 100 - ( ppromotion->value * 100 ) ), ppromotion->category, ppromotion->promoter_id );
		
		for ( int i = 0; i < pbackend->num_online_clients; i++ )
		{
			PIO pio = &pbackend->pclients[ i ].io;
			if ( pio->initialized )
				writeIoFifo( pio, ( char* ) &req, sizeof( req ) );
		}
		
		char announcement_buffer[276];
		sprintf( announcement_buffer, "\r\r[ Announcement ] %s\n", req.response.back.buffer );
		write( pbackend->announcements_pipe[ 1 ], announcement_buffer, strlen( announcement_buffer ) + 1 );
		
		// in case the timer is zeroed we delete the promotion
		//
		ppromotion = deletePromotion( ppromotion );
		ret++;
		
		if ( plast )
			plast->pnext = ppromotion;
		else
			pbackend->ppromotions = ppromotion;
		
		if ( !ppromotion )
			break;
	}
	
	pthread_mutex_unlock( &pbackend->promotions_mutex );
	
	return ret;
}

int processBackendClients( PBACKEND pbackend )
{
	if ( !pbackend )
		return -1;
	
	if ( !pbackend->num_online_clients )
		return 0;
	
	if ( pthread_mutex_lock( &pbackend->clients_mutex ) )
		return -1;
	
	time_t cur_time = time( NULL );
	
	int ret = 0;
	for ( int i = 0; i < pbackend->num_online_clients; i++ )
	{
		PCLIENT pclient = &pbackend->pclients[ i ];
		if ( pclient->last_hb_timestamp + pbackend->heartbeat + 1 > cur_time )
			continue;
		
		char sys_buff[256];
		sprintf( sys_buff, "\r\r[ System ] Removing client (%s) with process id: %d.\n",
		         pclient->username, pclient->pid );
		write( pbackend->announcements_pipe[ 1 ], sys_buff, strlen( sys_buff ) + 1 );
		
		if ( !removeClient( pbackend->pclients, pbackend->num_online_clients, pclient->pid ) )
		{
			perror( "Error Removing Client" );
			pthread_mutex_unlock( &pbackend->clients_mutex );
			exit( 1 );
		}
		
		pbackend->num_online_clients--;
	}
	
	pthread_mutex_unlock( &pbackend->clients_mutex );
	
	return ret;
}

PCLIENT getClientByPid( PCLIENT pclients, unsigned int num_clients, unsigned int pid )
{
	if ( !pclients || !num_clients )
		return NULL;
	
	for ( int i = 0; i < num_clients; i++ )
		if ( pclients[ i ].pid == pid )
			return &pclients[ i ];
	
	return NULL;
}

PCLIENT getClientByUsername( PCLIENT pclients, unsigned int num_clients, char* username )
{
	if ( !pclients || !num_clients || !username )
		return NULL;
	
	for ( int i = 0; i < num_clients; i++ )
		if ( strcmp( pclients[ i ].username, username ) == 0 )
			return &pclients[ i ];
	
	return NULL;
}

bool updateClientHeartbeat( PBACKEND pbackend, unsigned int pid )
{
	if ( !pbackend )
		return false;
	
	for ( int i = 0; i < pbackend->num_online_clients; i++ )
		if ( pbackend->pclients[ i ].pid == pid )
		{
			pbackend->pclients[ i ].last_hb_timestamp = time( NULL );
			return true;
		}
	
	return false;
}

bool bidBackendAuction( PBACKEND pbackend, PAUCTION pauction, PCLIENT pclient, unsigned int value )
{
	if ( !pauction || isAuctionFinished( pauction ) || value <= pauction->current_bid.value )
		return false;
	
	// Restore Older Bidder Cash
	if ( strcmp( pauction->current_bid.username, "-" ) != 0 )
	{
		int balance = getUserBalance( pauction->current_bid.username );
		if ( balance == -1 )
		{
			printf( "Error accessing User balance: %s\n", getLastErrorText( ) );
			return false;
		}
		
		balance += getAuctionFinalPrice( pauction );
		
		if ( updateUserBalance( pauction->current_bid.username, balance ) == -1 )
		{
			printf( "Error updating User balance: %s\n", getLastErrorText( ) );
			return false;
		}
	}
	
	// Update Bidder
	pauction->current_bid.value = value;
	strcpy( pauction->current_bid.username, pclient->username );
	
	PPROMOTION ppromotion = getPromotionByCategory( pbackend->ppromotions, pauction->category );
	pauction->current_bid.promotion_value = ppromotion ? ppromotion->value : 1.0f;
	
	int balance = getUserBalance( pclient->username );
	if ( balance == -1 )
	{
		printf( "Error accessing User balance: %s\n", getLastErrorText( ) );
		return false;
	}
	
	balance -= getAuctionFinalPrice( pauction );
	
	int ret = updateUserBalance( pclient->username, balance );
	if ( ret == -1 )
		printf( "Error updating User balance: %s\n", getLastErrorText( ) );
	
	return ret != -1;
}

bool buyNowBackendAuction( PBACKEND pbackend, PAUCTION pauction, PCLIENT pclient, unsigned int value )
{
	if ( !pauction || isAuctionFinished( pauction ) || value < pauction->buy_now_value )
		return false;
	
	// Restore Older Bidder Cash
	if ( strcmp( pauction->current_bid.username, "-" ) != 0 )
	{
		int balance = getUserBalance( pauction->current_bid.username );
		if ( balance == -1 )
		{
			printf( "Error accessing User balance: %s\n", getLastErrorText( ) );
			pthread_mutex_unlock( &pbackend->clients_mutex );
			pthread_mutex_unlock( &pbackend->auctions_mutex );
			exit( 1 );
		}
		
		balance += getAuctionFinalPrice( pauction );
		
		if ( updateUserBalance( pauction->current_bid.username, balance ) == -1 )
		{
			printf( "Error updating User balance: %s\n", getLastErrorText( ) );
			pthread_mutex_unlock( &pbackend->clients_mutex );
			pthread_mutex_unlock( &pbackend->auctions_mutex );
			exit( 1 );
		}
	}
	
	// update auction
	pauction->current_bid.value = value;
	strcpy( pauction->current_bid.username, pclient->username );
	
	if ( pthread_mutex_lock( &pbackend->promotions_mutex ) )
	{
		perror( "Error locking Promotions mutex" );
		pthread_mutex_unlock( &pbackend->clients_mutex );
		pthread_mutex_unlock( &pbackend->auctions_mutex );
		exit( 1 );
	}
	
	PPROMOTION ppromotion = getPromotionByCategory( pbackend->ppromotions, pauction->category );
	pauction->current_bid.promotion_value = ppromotion ? ppromotion->value : 1.0f;
	
	pthread_mutex_unlock( &pbackend->promotions_mutex );
	
	// finish auction
	pauction->timer = 0;
	
	// remove cash
	int balance = getUserBalance( pclient->username );
	if ( balance == -1 )
	{
		printf( "Error accessing User balance: %s\n", getLastErrorText( ) );
		pthread_mutex_unlock( &pbackend->clients_mutex );
		pthread_mutex_unlock( &pbackend->auctions_mutex );
		exit( 1 );
	}
	
	balance -= getAuctionFinalPrice( pauction );
	
	if ( updateUserBalance( pclient->username, balance ) == -1 )
	{
		printf( "Error updating User balance: %s\n", getLastErrorText( ) );
		pthread_mutex_unlock( &pbackend->clients_mutex );
		pthread_mutex_unlock( &pbackend->auctions_mutex );
		exit( 1 );
	}
	
	REQUEST req;
	memset( &req, 0, sizeof( REQUEST ) );
	req.to = -1;
	req.from = 0;
	req.type = REQ_PRINT;
	
	sprintf( req.response.back.buffer, "Congratulations %s for purchasing the auction item %s from %s for %d$!",
	         pauction->current_bid.username, pauction->name, pauction->current_bid.username,
	         getAuctionFinalPrice( pauction ) );
	
	
	for ( int i = 0; i < pbackend->num_online_clients; i++ )
	{
		PIO pio = &pbackend->pclients[ i ].io;
		if ( pio->initialized && strcmp( pauction->current_bid.username, pbackend->pclients[ i ].username ) != 0 )
			writeIoFifo( pio, ( char* ) &req, sizeof( req ) );
	}
	
	char announcement_buffer[276];
	sprintf( announcement_buffer, "\r\r[ Announcement ] %s\n", req.response.back.buffer );
	write( pbackend->announcements_pipe[ 1 ], announcement_buffer, strlen( announcement_buffer ) + 1 );
	
	return true;
}