#include <time.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <stdbool.h>

#include "Dialog/dialog.h"
#include "Vector/vector.h"
#include "InsParse/insparse.h"
#include "GameEngine/gameengine.h"
#include "SaveEngine/saveengine.h"

GAME_ENGINE game_engine = { 0 };

void exit_handler( void )
{
	if ( game_engine.state || !showConfirmMessage( "Do you wanna save the current game?" ) )
		return;

	SAVE_FILE cur_save;
	packSave( &cur_save, &game_engine );

	saveToFileSave( &cur_save, "jogo.bin" );
}

int main( void )
{
	// set random seed for rand function
	srand( ( unsigned int )time( NULL ) );

	printf( "Ultimate Tic-Tac-Toe\n\n" );

	SAVE_FILE cur_save;
	if ( loadFromFileSave( &cur_save, "jogo.bin" ) )
	{
		printf( "Attempting to load a save file from %s\n", ctime( &cur_save.save_time ) );
		if ( showConfirmMessage( "Do you wanna load it?" ) )
		{
			if ( !unpackSave( &cur_save, &game_engine ) )
			{
				showErrorMessage( "Unpacking save" );

				resetGameBoard( &game_engine );

				game_engine.type = getGameType( );

				getPlayerNames( &game_engine );
			}
		}
		else
		{
			resetGameBoard( &game_engine );

			game_engine.type = getGameType( );

			getPlayerNames( &game_engine );
		}
	}
	else
	{
		resetGameBoard( &game_engine );

		game_engine.type = getGameType( );

		getPlayerNames( &game_engine );
	}

	atexit( exit_handler );

	// game action loop
	char ins_buffer [200];
	INS_INFO ins_info = { 0 };
	ins_info.type = MARK;
	while ( true )
	{
		if ( ins_info.type == MARK )
		{
			printf( "\nCurrent Player: %s | Current Board: %d | Current Game State: %s\n\n\n",
					game_engine.players_name [game_engine.next_pmoving], game_engine.next_board_id,
					game_status [game_engine.state] );

			showGameBoard( &game_engine, game_engine.next_board_id );
		}

		printf( "Command: " );

		// obtaining instruction line
		fgets( ins_buffer, sizeof( ins_buffer ), stdin );

		printf( "\n" );

		// remove newline char
		if ( ins_buffer [strlen( ins_buffer ) - 1] == '\n' )
			ins_buffer [strlen( ins_buffer ) - 1] = '\0';

		// parsing instruction
		if ( !parseInstruction( ins_buffer, sizeof( ins_buffer ), &ins_info ) )
		{
			// changing type just in case it is MARK so we avoid the printf
			ins_info.type = SAVE;
			continue;
		}

		// executing instruction
		switch ( ins_info.type )
		{
		case SAVE:
		{
			SAVE_FILE cur_save;
			packSave( &cur_save, &game_engine );

			char* filename_buffer = ( char* )malloc( strlen( ins_info.filename ) + 5 );
			if ( !filename_buffer )
			{
				showErrorMessage( "Allocating memory" );
				break;
			}

			if ( sprintf( filename_buffer, "%s.bin", ins_info.filename ) < 0 )
			{
				showErrorMessage( "Formating file name" );

				free( filename_buffer );
				break;
			}

			saveToFileSave( &cur_save, filename_buffer );

			free( filename_buffer );
			break;
		}
		case LOAD:
		{
			char* filename_buffer = ( char* )malloc( strlen( ins_info.filename ) + 5 );
			if ( !filename_buffer )
			{
				showErrorMessage( "Allocating memory" );

				break;
			}

			if ( sprintf( filename_buffer, "%s.bin", ins_info.filename ) < 0 )
			{
				showErrorMessage( "Formating file name" );

				free( filename_buffer );
				break;
			}

			SAVE_FILE cur_save;
			if ( !loadFromFileSave( &cur_save, filename_buffer ) )
			{
				showErrorMessage( "Loading file from disk" );

				free( filename_buffer );
				break;
			}

			printf( "Attempting to load a save file from %s\n", ctime( &cur_save.save_time ) );
			if ( showConfirmMessage( "Do you wanna load it?" ) )
				if ( !unpackSave( &cur_save, &game_engine ) )
			{
				showErrorMessage( "Unpacking save, game compromised" );

				free( filename_buffer );

				GAME_TYPE type = game_engine.type;

				resetGameBoard( &game_engine );

				game_engine.type = type;

				getPlayerNames( &game_engine );

				ins_info.type = MARK;

				break;
			}

			ins_info.type = MARK;
			break;
		}
		case MARK:
		{
			if ( game_engine.state )
			{
				showErrorMessage( "The game is aleardy over" );

				break;
			}

			if ( !markSquareGameBoard( &game_engine, ins_info.place - 1 ) )
			{
				showErrorMessage( "Invalid square" );

				break;
			}

			break;
		}
		case VIEW:
		{
			if ( !showGameBoard( &game_engine, 0 ) )
				return 1;
			break;
		}
		case HISTORY:
		{
			printLastMovesGameBoard( &game_engine, ins_info.num_plays );
			break;
		}
		case TYPE:
			if ( ins_info.new_game_type == game_engine.type )
				break;

			game_engine.type = !game_engine.type;
		case RESTART:
		{
			GAME_TYPE type = game_engine.type;

			resetGameBoard( &game_engine );

			game_engine.type = type;

			getPlayerNames( &game_engine );

			ins_info.type = MARK;

			break;
		}
		case EXIT:
			return 0;
		case HELP:
		{
			for ( int i = 0; i < HELP + 1; i++ )
				printf( "%s", help_messages [i] );
			break;
		}
		default:
			break;
		}

		processGameBoard( &game_engine );
	}

	return 0;
}