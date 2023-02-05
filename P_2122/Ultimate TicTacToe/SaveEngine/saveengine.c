#include <time.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <stdbool.h>

#include "../Dialog/dialog.h"

#include "saveengine.h"

// packSave
// this function will pack a save structure
//
void packSave( PSAVE_FILE psave_file, PGAME_ENGINE pgame_engine )
{
	psave_file->magic_value = ( int )'Z74M';

	psave_file->type = pgame_engine->type;
	psave_file->state = pgame_engine->state;
	psave_file->next_pmoving = pgame_engine->next_pmoving;
	psave_file->next_board_id = pgame_engine->next_board_id;
	memset( psave_file->board_moves, 0, sizeof( psave_file->board_moves ) );
	memcpy( psave_file->player_names, pgame_engine->players_name, sizeof( psave_file->player_names ) );

	psave_file->num_moves = pgame_engine->num_moves;

	int i = 0;
	for ( PBOARD_MOVE pmove = pgame_engine->board_moves_list; pmove; pmove = pmove->pnext, i++ )
		psave_file->board_moves [i] = *pmove;

	time( &psave_file->save_time );
}

// unpackSave
// this function will unpack a save structure
//
bool unpackSave( PSAVE_FILE psave_file, PGAME_ENGINE pgame_engine )
{
	if ( psave_file->magic_value != 'Z74M' )
	{
		showErrorMessage( "Invalid save filee" );

		return false;
	}

	resetGameBoard( pgame_engine );

	pgame_engine->board_moves_list = ( PBOARD_MOVE )malloc( sizeof( BOARD_MOVE ) );
	if ( !pgame_engine->board_moves_list )
	{
		showErrorMessage( "Allocating memory" );

		return false;
	}
	memset( pgame_engine->board_moves_list, 0, sizeof( BOARD_MOVE ) );

	pgame_engine->type = psave_file->type;
	pgame_engine->state = psave_file->state;
	pgame_engine->num_moves = psave_file->num_moves;
	pgame_engine->next_pmoving = psave_file->next_pmoving;
	pgame_engine->next_board_id = psave_file->next_board_id;
	memcpy( pgame_engine->players_name, psave_file->player_names, sizeof( psave_file->player_names ) );

	PBOARD pboards = getBoardsBuffer( pgame_engine );
	if ( !pboards )
	{
		showErrorMessage( "Obtaining boards" );

		return false;
	}

	// writee new moves
	unsigned int i = 0;
	for ( PBOARD_MOVE pmove = pgame_engine->board_moves_list; i < psave_file->num_moves; i++, pmove = pmove->pnext )
	{
		pmove->state = psave_file->board_moves [i].state;
		pmove->board_id = psave_file->board_moves [i].board_id;
		pmove->square_id = psave_file->board_moves [i].square_id;

		pboards [pmove->board_id].squares[pmove->square_id] = pmove->state;

		if ( i == psave_file->num_moves - 1 )
			continue;
		
		pmove->pnext = ( PBOARD_MOVE )malloc( sizeof( BOARD_MOVE ) );

		if ( !pmove->pnext )
		{
			showErrorMessage( "Allocating memory" );

			return false;
		}

		memset( pmove->pnext, 0, sizeof( BOARD_MOVE ) );
	}

	return true;
}

// saveToFileSave
// this function will save the save structure to the specified file
//
bool saveToFileSave( PSAVE_FILE psave_file, char* filename )
{
	if ( !filename )
	{
		showErrorMessage( "Invalid file name" );

		return false;
	}

	FILE* pfile = fopen( filename, "rb" );
	if ( pfile )
	{
		SAVE_FILE temp_save;
		fread( &temp_save, sizeof( SAVE_FILE ), 1, pfile );

		fclose( pfile );

		printf( "Attempting to replace a save file from %s\n", ctime( &temp_save.save_time ) );

		if ( !showConfirmMessage( "Do you wanna replace the old save file for this new one? " ) )
			return false;
	}

	pfile = fopen( filename, "wb" );
	if ( !pfile )
		return false;

	fwrite( psave_file, sizeof( SAVE_FILE ), 1, pfile );
	fclose( pfile );

	return true;
}

// loadFromFileSave
// this function will load the save structure from the specified file
//
bool loadFromFileSave( PSAVE_FILE psave_file, char* filename )
{
	if ( !filename )
	{
		showErrorMessage( "Invalid file name" );

		return false;
	}

	FILE* pfile = fopen( filename, "rb" );
	if ( !pfile )
		return false;

	fread( psave_file, sizeof( SAVE_FILE ), 1, pfile );
	fclose( pfile );

	return true;
}