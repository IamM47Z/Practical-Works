#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdbool.h>

#include "../Misc/misc.h"

#include "insparse.h"

// all instruction type names
//
const char* instruction_types [] = { "SAVE", "LOAD", "TYPE", "MARK", "VIEW", "HISTORY", "RESTART", "EXIT", "HELP" };

// all instruction help messages
//
const char* help_messages [HELP + 1] = { "SAVE <filename> saves the game into the file <filename>.bin\n", "LOAD <filename> loads the save file <filename>.bin\n",
			"TYPE <multiplayer (mp)/singleplayer (sp)> changes the game mode\n", "MARK <place (1-9)> sets the square number <placee (1-9)> at the current board\n",
			"VIEW shows all the boards in the game\n", "HISTORY <num of plays (1-10)> shows <num of plays (1-10)> of game moves\n",
			"RESTART restarts the match\n", "EXIT closes the game\n", "HELP shows all commands info\n\n" };

// parseInstruction
// this function will parse the given command line
// 
bool parseInstruction( char* ins_buffer, size_t buffer_size, PINS_INFO pout )
{
	// sanity checks
	if ( !pout || buffer_size < 1 || strlen( ins_buffer ) < 1 )
		return false;

	// duping original buffer to dont mess with it
	char* temp_ins_buffer = ( char* )malloc( buffer_size );
	if ( !temp_ins_buffer )
		return false;

	strcpy( temp_ins_buffer, ins_buffer );

	// parsing string
	char* ins_type = strtok( temp_ins_buffer, " " );
	if ( !ins_type )
		return false;

	// checking in original string 
	char* ins_params = ins_type + strlen( ins_type ) + ( strlen( ins_buffer ) > strlen( ins_type ) ? 1 : 0 );

	// filling out struct
	bool status = true;

	if ( !istrcmp( ins_type, "save" ) )
	{
		pout->type = SAVE;

		if ( !isValidFileName( ins_params ) )
			status = false;
		else
			strcpy( pout->filename, ins_params );
	}
	else if ( !istrcmp( ins_type, "load" ) )
	{
		pout->type = LOAD;

		if ( !isValidFileName( ins_params ) )
			status = false;
		else
			strcpy( pout->filename, ins_params );
	}
	else if ( !istrcmp( ins_type, "type" ) )
	{
		pout->type = TYPE;

		if ( istrcmp( ins_params, "mp" ) && istrcmp( ins_params, "sp" ) &&
			 istrcmp( ins_params, "multiplayer" ) && istrcmp( ins_params, "singleplayer" ) )
			status = false;
		else
			pout->new_game_type = ( !istrcmp( ins_params, "mp" ) || !istrcmp( ins_params, "multiplayer" ) ) ? MULTIPLAYER : SINGLEPLAYER;
	}
	else if ( !istrcmp( ins_type, "mark" ) )
	{
		pout->type = MARK;

		if ( atoi( ins_params ) < 1 || atoi( ins_params ) > 9 )
			status = false;
		else
			pout->place = atoi( ins_params );
	}
	else if ( !istrcmp( ins_type, "view" ) )
	{
		pout->type = VIEW;

		if ( strlen( ins_params ) )
			status = false;
	}
	else if ( !istrcmp( ins_type, "history" ) )
	{
		pout->type = HISTORY;

		if ( atoi( ins_params ) > 10 || atoi( ins_params ) < 1 )
			status = false;
		else
			pout->num_plays = atoi( ins_params );
	}
	else if ( !istrcmp( ins_type, "restart" ) )
	{
		pout->type = RESTART;

		if ( strlen( ins_params ) )
			status = false;
	}
	else if ( !istrcmp( ins_type, "exit" ) )
	{
		pout->type = EXIT;

		if ( strlen( ins_params ) )
			status = false;
	}
	else if ( !istrcmp( ins_type, "help" ) )
	{
		pout->type = HELP;

		if ( strlen( ins_params ) )
			status = false;
	}
	else
	{
		// releasing resources
		free( temp_ins_buffer );

		return false;
	}

	// releasing resources
	free( temp_ins_buffer );

	// show how to use the attempted instruction in case the type existes
	if ( !status )
		showInstructionSyntax( pout->type );

	return status;
}

// showInstructionSyntax
// this function will show the proper way to call the given instruction 
//
void showInstructionSyntax( INS_TYPE type )
{
	printf( "%s\n\n%s\n", instruction_types [type], help_messages [type] );
}