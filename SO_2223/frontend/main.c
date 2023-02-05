#include <stdio.h>
#include <errno.h>
#include <signal.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include "frontend/frontend.h"
#include "insparse/insparse.h"
#include "io/io.h"

PFRONTEND pfrontend = NULL;

void exitHandler( )
{
	if ( pfrontend )
	{
		deleteFrontend( pfrontend );
		pfrontend = NULL;
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
	if ( argc != 3 )
		return 1;
	
	// register our handlers
	//
	atexit( exitHandler );
	
	signal( SIGINT, signalHandler );
	signal( SIGHUP, signalHandler );
	signal( SIGQUIT, signalHandler );
	signal( SIGTERM, signalHandler );
	signal( SIGABRT, signalHandler );
	signal( SIGPIPE, signalHandler );
	
	printf( "SOBay - FrontEnd - Client Process ID: %d\n", getpid( ) );
	
	if ( !( pfrontend = createFrontend( argv[ 1 ], argv[ 2 ] ) ) )
		return 1;
	
	if ( !loginFrontend( pfrontend ) )
		return 1;
	
	// loop both fd's
	//
	printf( "\nCommand: " );
	fflush( stdout );
	
	fd_set read_fds;
	
	char ins_buffer[200];
	INS_INFO ins_info = { 0 };
	while ( ins_info.type != EXIT )
	{
		// initialize set
		//
		FD_ZERO( &read_fds );
		FD_SET( STDIN_FILENO, &read_fds );
		FD_SET( pfrontend->client_io.fd, &read_fds );
		
		int nfds = select( pfrontend->client_io.fd + 1, &read_fds, NULL, NULL, NULL );
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
			
			// creating request
			REQUEST req;
			memset( &req, 0, sizeof( REQUEST ) );
			
			// executing instruction
			bool needs_request = true;
			switch ( ins_info.type )
			{
				case SELL:
				{
					req.from = getpid( );
					req.to = 0;
					req.type = REQ_SELL;
					req.response.front.sell.price = ins_info.price;
					req.response.front.sell.duration = ins_info.duration;
					req.response.front.sell.buy_now_price = ins_info.buy_now_price;
					
					strcpy( req.response.front.sell.name, ins_info.name );
					strcpy( req.response.front.sell.category, ins_info.category );
					break;
				}
				case LIST:
				{
					req.from = getpid( );
					req.to = 0;
					req.type = REQ_LIST;
					break;
				}
				case LICAT:
				{
					req.from = getpid( );
					req.to = 0;
					req.type = REQ_LIST;
					req.response.front.list.by_cat = true;
					strcpy( req.response.front.list.cat, ins_info.buffer );
					break;
				}
				case LISEL:
				{
					req.from = getpid( );
					req.to = 0;
					req.type = REQ_LIST;
					req.response.front.list.by_user = true;
					strcpy( req.response.front.list.user, ins_info.buffer );
					break;
				}
				case LIVAL:
				{
					req.from = getpid( );
					req.to = 0;
					req.type = REQ_LIST;
					req.response.front.list.by_price = true;
					req.response.front.list.price = ins_info.int_value;
					break;
				}
				case LITIME:
				{
					req.from = getpid( );
					req.to = 0;
					req.type = REQ_LIST;
					req.response.front.list.by_time = true;
					req.response.front.list.time = ins_info.int_value;
					break;
				}
				case TIME:
				{
					req.from = getpid( );
					req.to = 0;
					req.type = REQ_TIME;
					break;
				}
				case BUY:
				{
					req.from = getpid( );
					req.to = 0;
					req.type = REQ_BID;
					req.response.front.bid.auction_id = ins_info.id;
					req.response.front.bid.value = ins_info.bid_value;
					break;
				}
				case CASH:
				{
					req.from = getpid( );
					req.to = 0;
					req.type = REQ_BALANCE;
					break;
				}
				case ADD:
				{
					req.from = getpid( );
					req.to = 0;
					req.type = REQ_ADD;
					req.response.front.add.value = ins_info.int_value;
					break;
				}
				case EXIT:
				{
					needs_request = false;
					exit( 0 );
				}
				case HELP:
				{
					needs_request = false;
					
					printf( "\n" );
					for ( int i = 0; i < HELP + 1; i++ )
						printf( "%s", help_messages[ i ] );
					printf( "\n\nCommand: " );
					fflush( stdout );
					fflush( stdin );
					break;
				}
				default:
					needs_request = false;
					break;
			}
			
			if ( needs_request && writeIoFifo( &pfrontend->server_io, ( char* ) &req, sizeof( REQUEST ) ) == -1 )
			{
				perror( "Error writing to server Fifo" );
				exit( 1 );
			}
		}
		
		if ( FD_ISSET( pfrontend->client_io.fd, &read_fds ) )
		{
			bool finished = false, has_written = false;
			while ( !finished )
			{
				REQUEST req;
				memset( &req, 0, sizeof( REQUEST ) );
				switch ( readIoFifo( &pfrontend->client_io, ( char* ) &req, sizeof( REQUEST ) ) )
				{
					case -1:
					{
						finished = true;
						
						if ( errno == EWOULDBLOCK )
							continue;
						
						perror( "Error reading Fifo" );
						exit( 1 );
					}
					case 0:
						finished = true;
						break;
					default:
					{
						if ( req.to != -1 && req.to != getpid( ) )
							continue;
						
						switch ( req.type )
						{
							case REQ_SELL:
							{
								has_written = true;
								if ( req.response.status )
									printf( "\nItem successfully put up for Auction.\n\n" );
								else
									printf( "\nError putting up the item for Auction.\n\n" );
								
								break;
							}
							case REQ_LIST:
							{
								has_written = true;
								if ( req.response.status )
								{
									printf( "\nAuctions: \n" );
									if ( req.response.back.list.num == 0 )
										printf( "None\n" );
									else
										for ( int i = 0; i < req.response.back.list.num; i++ )
										{
											AUCTION auction = req.response.back.list.auctions[ i ];
											printf( "Item [ %d ] %s (%s) is up for Auction (Buy Now: %d $)\n\tSeller: %s\n\tLast Bidder: %s - %d$\n\tRemaining Time: %d scs\n\n",
											        auction.item_id, auction.name, auction.category,
											        auction.buy_now_value,
											        auction.seller_name,
											        auction.current_bid.username,
											        ( int ) ( ( float ) auction.current_bid.value *
											                  auction.current_bid.promotion_value ),
											        auction.timer );
										}
									
									printf( "\n" );
								}
								else
									printf( "\nError listing items up for Auction.\n\n" );
								break;
							}
							case REQ_TIME:
							{
								has_written = true;
								if ( req.response.status )
									printf( "\nThe Server uptime is: %d scs.\n\n", req.response.back.uptime );
								else
									printf( "\nThe Server uptime could not be retrieved.\n\n" );
								break;
							}
							case REQ_BID:
							{
								has_written = true;
								printf( "\n" );
								
								if ( req.response.back.purchased )
								{
									if ( req.response.status )
										printf( "Congratulations, the item was purchased successfully!\n\n" );
									else
										printf( "Error purchasing the item.\n\n" );
								}
								else
								{
									if ( req.response.status )
										printf( "Bid placed successfully.\n\n" );
									else
										printf( "Error placing bid.\n\n" );
								}
								break;
							}
							case REQ_BALANCE:
							{
								has_written = true;
								
								if ( req.response.status )
									printf( "\nCurrent Balance: %d $.\n\n", req.response.back.balance );
								else
									printf( "\nThe Current Balance could not be retrieved.\n\n" );
								break;
							}
							case REQ_ADD:
							{
								has_written = true;
								
								if ( req.response.status )
									printf( "\nBalanced updated successfully.\n\n" );
								else
									printf( "\nError updating balance.\n\n" );
								break;
							}
							case REQ_DISCONNECT:
							{
								has_written = true;
								
								printf( "\n\nBackend has closed or you have been kicked!\n" );
								
								if ( logoutFrontend( pfrontend ) )
								{
									printf( "Logged out Successfully!\n" );
									exit( 0 );
								}
								
								perror( "Error logging out!" );
								exit( 1 );
							}
							case REQ_PRINT:
							{
								has_written = true;
								printf( "\r\r[ Server Announcement ] %s\n", req.response.back.buffer );
								break;
							}
							default:
								break;
						}
					}
				}
			}
			
			if ( has_written )
			{
				printf( "Command: " );
				fflush( stdout );
				fflush( stdin );
			}
		}
	}
	
	return 0;
}
