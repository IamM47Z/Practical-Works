#include <time.h>
#include <stdio.h>
#include <string.h>

#include "../users/users_lib.h"

#include "client.h"

bool addClient( PCLIENT client_list, unsigned int num_clients,
                char* pathname, char* username, char* password, int pid )
{
	if ( !client_list || !username || !password || strlen( username ) > 24 || strlen( password ) > 24 )
		return false;
	
	for ( int i = 0; i < num_clients; i++ )
		if ( client_list[ i ].pid == pid || !strcmp( client_list[ i ].username, username ) )
			return false;
	
	int ret = isUserValid( username, password );
	if ( ret == -1 )
	{
		perror( "Error using Users library" );
		exit( 1 );
	}
	
	if ( !ret )
		return false;
	
	if ( !initializeIo( &client_list[ num_clients ].io, pathname, O_WRONLY ) )
		return false;
	
	client_list[ num_clients ].pid = pid;
	client_list[ num_clients ].last_hb_timestamp = time( NULL );
	
	strcpy( client_list[ num_clients ].username, username );
	
	return true;
}

bool removeClient( PCLIENT client_list, unsigned int num_clients, int pid )
{
	if ( !client_list || num_clients < 1 )
		return false;
	
	for ( int i = 0; i < num_clients; i++ )
		if ( client_list[ i ].pid == pid )
		{
			if ( i == num_clients - 1 )
				memset( &client_list[ i ], 0, sizeof( CLIENT ) );
			else
			{
				memmove( &client_list[ i ], &client_list[ i + 1 ], sizeof( CLIENT ) * ( num_clients - i - 1 ) );
				memset( &client_list[ num_clients - 1 ], 0, sizeof( CLIENT ) );
			}
			
			return true;
		}
	
	return false;
}
