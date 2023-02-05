#include <stdio.h>
#include <string.h>
#include <stdbool.h>

#include "../Misc/misc.h"

#include "dialog.h"

GAME_TYPE getGameType( )
{
	printf( "Singleplayer(sp) or Multiplayer(mp): " );

	char type_buffer [256];
	fgets( type_buffer, sizeof( type_buffer ), stdin );
	if ( type_buffer [strlen( type_buffer ) - 1] == '\n' )
		type_buffer [strlen( type_buffer ) - 1] = '\0';

	if ( istrcmp( type_buffer, "mp" ) && istrcmp( type_buffer, "sp" ) &&
		 istrcmp( type_buffer, "multiplayer" ) && istrcmp( type_buffer, "singleplayer" ) )
		return getGameType( );

	return ( !istrcmp( type_buffer, "mp" ) || !istrcmp( type_buffer, "multiplayer" ) ) ? MULTIPLAYER : SINGLEPLAYER;
}

void showErrorMessage( char* str )
{
	printf( "[Error] %s\n", str );
}

bool showConfirmMessage( char* str )
{
	printf( "%s (y/n): ", str );

	char buffer [5];
	fgets( buffer, sizeof( buffer ), stdin );
	if ( buffer [strlen( buffer ) - 1] == '\n' )
		buffer [strlen( buffer ) - 1] = '\0';

	if ( istrcmp( buffer, "y" ) && istrcmp( buffer, "yes" ) && istrcmp( buffer, "n" ) && istrcmp( buffer, "no" ) )
		return showConfirmMessage( str );

	printf( "\n" );

	return !istrcmp( buffer, "y" ) || !istrcmp( buffer, "yes" );
}

void getPlayerNames( PGAME_ENGINE pgame_engine )
{
	printf( "Player Number 1 Nickname (16 chars max): " );
	fgets( pgame_engine->players_name [1], sizeof( pgame_engine->players_name [1] ), stdin );
	if ( pgame_engine->players_name [1][strlen( pgame_engine->players_name [1] ) - 1] == '\n' )
		pgame_engine->players_name [1][strlen( pgame_engine->players_name [1] ) - 1] = '\0';

	if ( !pgame_engine->type )
		return;

	printf( "Player Number 2 Nickname (16 chars max): " );
	fgets( pgame_engine->players_name [2], sizeof( pgame_engine->players_name [2] ), stdin );
	if ( pgame_engine->players_name [2][strlen( pgame_engine->players_name [2] ) - 1] == '\n' )
		pgame_engine->players_name [2][strlen( pgame_engine->players_name [2] ) - 1] = '\0';
}