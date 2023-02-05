#include <stdio.h>
#include <stdarg.h>
#include <string.h>
#include <stdlib.h>
#include <stdbool.h>

#include "../Misc/misc.h"
#include "../Dialog/dialog.h"

#include "gameengine.h"

const char* piece_states [] = { " ", "X", "O" };
const char* game_status [DRAW + 1] = { "Playing", "Victory Player 1", "Victory Player 2", "Draw" };

// showGameBoard
// this function will print the board by board_id or all the boards 
//
bool showGameBoard( PGAME_ENGINE pgame_engine, unsigned int board_id )
{
	PBOARD pboards = getBoardsBuffer( pgame_engine );

	// sanity check
	if ( board_id < 0 || board_id > 9 || !pboards )
		return false;

	printf( "\n" );

	// in case board_id is 0 we print the whole board
	if ( !board_id )
		for ( int i = 0; i < 9; i += 3 )
		{
			if ( !i )
				printf( "\t                          |                           |\n" );

			for ( int j = 0; j < 9; j += 3 )
			{
				printf( "\t        |       |         |         |       |         |         |       |\n" );
				printf( "\t    %s   |   %s   |   %s     |     %s   |   %s   |   %s     |     %s   |   %s   |   %s\n",
						piece_states [pboards [i].squares [j]],
						piece_states [pboards [i].squares [j + 1]],
						piece_states [pboards [i].squares [j + 2]],

						piece_states [pboards [i + 1].squares [j]],
						piece_states [pboards [i + 1].squares [j + 1]],
						piece_states [pboards [i + 1].squares [j + 2]],

						piece_states [pboards [i + 2].squares [j]],
						piece_states [pboards [i + 2].squares [j + 1]],
						piece_states [pboards [i + 2].squares [j + 2]] );
				printf( "\t        |       |         |         |       |         |         |       |\n" );

				if ( j != 6 )
					printf( "\t -------|-------|-------  |  -------|-------|-------  |  -------|-------|------- \n" );
			}

			if ( i != 6 )
				printf( "\t                          |                           |\n\t--------------------------|---------------------------|--------------------------\n" );
			
			printf( "\t                          |                           |\n" );
		}
	// otherwise we print the specific board
	else
		for ( int j = 0; j < 9; j += 3 )
		{
			printf( "\t        |       |\n" );
			printf( "\t    %s   |   %s   |   %s\n",
					piece_states [pboards [board_id - 1].squares[j]],
					piece_states [pboards [board_id - 1].squares [j + 1]],
					piece_states [pboards [board_id - 1].squares [j + 2]] );
			printf( "\t        |       |\n" );

			if ( j != 6 )
				printf( "\t -------|-------|-------\n" );
		}

	printf( "\n" );

	return true;
}

// markSquareGameBoard
// this function will mark the squares in the current board
//
bool markSquareGameBoard( PGAME_ENGINE pgame_engine, unsigned int square_id )
{
	PBOARD pboards = getBoardsBuffer( pgame_engine );

	if ( square_id < 0 || square_id > 8 || !pboards || pboards [pgame_engine->next_board_id - 1].state ||
		 pboards [pgame_engine->next_board_id - 1].squares[square_id] || pgame_engine->state )
		return false;

	pboards [pgame_engine->next_board_id - 1].squares[square_id] = !pgame_engine->next_pmoving ? 2 : pgame_engine->next_pmoving;

	printf( "%s marked square %d\n", pgame_engine->players_name [pgame_engine->next_pmoving], square_id + 1 );

	PBOARD_MOVE new_move = ( PBOARD_MOVE )malloc( sizeof( BOARD_MOVE ) );
	if ( !new_move )
		return false;
	
	new_move->board_id = pgame_engine->next_board_id - 1;
	new_move->square_id = square_id;
	new_move->state = !pgame_engine->next_pmoving ? 2 : pgame_engine->next_pmoving;
	new_move->pnext = NULL;

	if ( pgame_engine->next_pmoving == 2 )
		pgame_engine->next_pmoving--;
	else if ( pgame_engine->type == SINGLEPLAYER && pgame_engine->next_pmoving == 1 )
		pgame_engine->next_pmoving--;
	else
		pgame_engine->next_pmoving++;

	if ( pgame_engine->board_moves_list )
	{
		for ( PBOARD_MOVE ptemp = pgame_engine->board_moves_list; ptemp; ptemp = ptemp->pnext )
			if ( !ptemp->pnext )
			{
				ptemp->pnext = new_move;
				break;
			}
	}
	else
		pgame_engine->board_moves_list = new_move;

	pgame_engine->num_moves++;
	pgame_engine->next_board_id = square_id + 1;

	return true;
}

// resetGameBoard 
// this function will reset the engine structure
//
void resetGameBoard( PGAME_ENGINE pgame_engine )
{
	for ( PBOARD_MOVE pmove = pgame_engine->board_moves_list; pmove; )
	{
		PBOARD_MOVE ptemp = pmove;
		pmove = pmove->pnext;
		free( ptemp );
	}

	if ( pgame_engine->boards )
		deleteVec( pgame_engine->boards );

	memset( pgame_engine, 0, sizeof( GAME_ENGINE ) );

	pgame_engine->next_pmoving = 1;
	pgame_engine->type = true;
	pgame_engine->next_board_id = 1;

	pgame_engine->boards = initializeVec( sizeof( BOARD ), 9 );
	if ( !pgame_engine->boards )
	{
		showErrorMessage( "Fatal error initializing game" );

		exit( 1 );
	}

	strcpy( pgame_engine->players_name [0], "[BOT] Laurinda" );
}

// processGameBoard
// this function will handle the game status changes and make the bot moves
//
void processGameBoard( PGAME_ENGINE pgame_engine )
{
	if ( pgame_engine->state )
		return;

	PBOARD pboards = getBoardsBuffer( pgame_engine );
	if ( !pboards )
		return;

	// singular board status checker
	for ( int i = 0; i < 9; i++ )
		checkSingularBoard( &pboards [i] );

	// game board status checker
	checkGameBoard( pgame_engine );

	// if game state defined than save file
	if ( pgame_engine->state )
	{
		printf( "\n%s\n\n", game_status [pgame_engine->state] );

		char buffer [100];

		do
		{
			printf( "Insert the file name to save this match history: \n" );
			fgets( buffer, sizeof( buffer ), stdin );
			if ( buffer [strlen( buffer ) - 1] == '\n' )
				buffer [strlen( buffer ) - 1] = '\0';
		} while ( !isValidFileName( buffer ) );

		char filename [104];
		sprintf( filename, "%s.txt", buffer );

		FILE* pfile = fopen( filename, "r" );
		if ( pfile && !showConfirmMessage( "Do you wanna replace the old history file for this new one? " ) )
			return;

		pfile = fopen( filename, "w" );
		if ( !pfile )
			return;

		PBOARD_MOVE pmove = pgame_engine->board_moves_list;
		for ( int i = 0; i < ( ( int )pgame_engine->num_moves - 1 ) && pmove; i++, pmove = pmove->pnext )
			fprintf( pfile, "%s marked on the board %d the square %d\n",
					pgame_engine->players_name [( pgame_engine->type == SINGLEPLAYER && pmove->state == 2 ) ? 0 : pmove->state],
					pmove->board_id + 1, pmove->square_id + 1 );

		// print the last one without the newline
		fprintf( pfile, "%s marked on the board %d the square %d",
				 pgame_engine->players_name [( pgame_engine->type == SINGLEPLAYER && pmove->state == 2 ) ? 0 : pmove->state],
				 pmove->board_id + 1, pmove->square_id + 1 );

		fclose( pfile );

		return;
	}

	// randomly chose a new board in case this one aleardy has a state
	if ( pboards [pgame_engine->next_board_id - 1].state )
	{
		int num_available = 0;
		int available_boards [9] = { 0 };
		for ( int i = 0; i < 9; i++ )
		{
			if ( pboards [i].state )
				continue;

			available_boards [num_available] = i;
			num_available++;
		}

		pgame_engine->next_board_id = available_boards [rand( ) % ( num_available + 1 )] + 1;
	}

	// our bot 
	if ( !pgame_engine->next_pmoving && !pgame_engine->type )
	{
		int num_available = 0;
		int available_squares [9] = { -1 };
		for ( int i = 0; i < 9; i++ )
		{
			if ( pboards [pgame_engine->next_board_id - 1].squares [i] )
				continue;

			available_squares [num_available] = i;
			num_available++;
		}

		int target_square = available_squares [rand( ) % ( num_available + 1 )];

		markSquareGameBoard( pgame_engine, target_square );

		processGameBoard( pgame_engine );
	}
}

// checkSingularBoard
// this function will check our board status
//
void checkSingularBoard( PBOARD pboard )
{
	if ( pboard->state )
		return;

	// check board rows and columns
	for ( int i = 0; i < 3; i++ )
	{
		bool has_result = true;

		// check our rows
		if ( pboard->squares [i * 3] != NONE )
		{
			for ( int j = 1; j < 3; j++ )
			{
				if ( pboard->squares [i * 3] == pboard->squares [i * 3 + j] )
					continue;

				has_result = false;
				break;
			}

			if ( has_result )
			{
				pboard->state = pboard->squares [i * 3];
				return;
			}

			has_result = true;
		}

		// check our columns	
		if ( pboard->squares [i] == NONE )
			continue;

		for ( int j = 1; j < 3; j++ )
		{
			if ( pboard->squares [i] == pboard->squares [j * 3 + i] )
				continue;

			has_result = false;
			break;
		}

		if ( has_result )
		{
			pboard->state = pboard->squares [i];
			return;
		}
	}

	bool has_result = true;

	// check our first diagonal
	if ( pboard->squares [0] != NONE )
	{
		for ( int i = 1; i < 3; i++ )
		{
			if ( pboard->squares [0] == pboard->squares [i * 3 + i] )
				continue;

			has_result = false;
			break;
		}

		if ( has_result )
		{
			pboard->state = pboard->squares [0];
			return;
		}

		has_result = true;
	}

	// check our second diagonal
	if ( pboard->squares [2] != NONE )
	{
		for ( int i = 0; i < 2; i++ )
		{
			if ( pboard->squares [2] == pboard->squares [( 2 - i ) * 3 + i] )
				continue;

			has_result = false;
			break;
		}

		if ( has_result )
		{
			pboard->state = pboard->squares [2];
			return;
		}
	}

	// check draw 
	pboard->state = DRAW;
	for ( int i = 0; i < 9; i++ )
	{
		if ( pboard->squares [i] )
			continue;

		pboard->state = NONE;
		break;
	}
}

// checkGameBoard
// this function will check our game board status
//
void checkGameBoard( PGAME_ENGINE pgame_engine )
{
	if ( pgame_engine->state )
		return;

	// obtain our boards buffer
	PBOARD pboards = getBoardsBuffer( pgame_engine );
	if ( !pboards )
		return;

	// check board rows and columns
	for ( int i = 0; i < 3; i++ )
	{
		bool has_result = true;

		// check our rows
		if ( pboards[i * 3].state != NONE )
		{
			for ( int j = 1; j < 3; j++ )
			{
				if ( pboards [i * 3].state == pboards [i * 3 + j].state )
					continue;

				has_result = false;
				break;
			}

			if ( has_result )
			{
				pgame_engine->state = pboards [i * 3].state;
				return;
			}

			has_result = true;
		}

		// check our columns	
		if ( pboards [i].state == NONE )
			continue;

		for ( int j = 1; j < 3; j++ )
		{
			if ( pboards [i].state == pboards [j * 3 + i].state )
				continue;

			has_result = false;
			break;
		}

		if ( has_result )
		{
			pgame_engine->state = pboards [i].state;
			return;
		}
	}

	bool has_result = true;

	// check our first diagonal
	if ( pboards [0].state != NONE )
	{
		for ( int i = 1; i < 3; i++ )
		{
			if ( pboards [0].state == pboards [i * 3 + i].state )
				continue;

			has_result = false;
			break;
		}

		if ( has_result )
		{
			pgame_engine->state = pboards [0].state;
			return;
		}

		has_result = true;
	}

	// check our second diagonal
	if ( pboards [2].state != NONE )
	{
		for ( int i = 0; i < 2; i++ )
		{
			if ( pboards [2].state == pboards [( 2 - i ) * 3 + i].state )
				continue;

			has_result = false;
			break;
		}

		if ( has_result )
		{
			pgame_engine->state = pboards [2].state;
			return;
		}
	}

	// check draw
	pgame_engine->state = DRAW;
	for ( int i = 0; i < 9; i++ )
	{
		if ( pboards [i].state )
			continue;

		pgame_engine->state = NONE;
		break;
	}
}

// printLastMovesGameBoard
// this function prints the last moves
//
void printLastMovesGameBoard( PGAME_ENGINE pgame_engine, unsigned int num_plays )
{
	if ( num_plays < 1 || num_plays > 10 )
		return;

	PBOARD_MOVE pmove = pgame_engine->board_moves_list;
	for ( int i = 0; i < ( int )pgame_engine->num_moves && pmove; i++, pmove = pmove->pnext )
	{
		// convert to int because subtraction can be negative
		if ( i < ( int )pgame_engine->num_moves - ( int )num_plays )
			continue;

		printf( "%s marked on the board %d the square %d\n", 
				pgame_engine->players_name [( pgame_engine->type == SINGLEPLAYER && pmove->state == 2 ) ? 0 : pmove->state], 
				pmove->board_id + 1, pmove->square_id + 1 );
	}

	printf( "\n" );
}

// getBoardsBuffere
// this function obtains the buffer to our boards
//
PBOARD getBoardsBuffer( PGAME_ENGINE pgame_engine )
{
	return ( !pgame_engine || !pgame_engine->boards ) ? NULL : ( PBOARD )pgame_engine->boards->buffer;
}