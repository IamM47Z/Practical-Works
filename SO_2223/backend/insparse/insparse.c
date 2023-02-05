#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdbool.h>
#include <ctype.h>

#include "insparse.h"

// all instruction type names
//
const char* instruction_types[] = { "USERS", "LIST", "KICK", "PROM", "REPROM", "CANCEL", "CLOSE", "HELP" };

// all instruction help messages
//
const char* help_messages[HELP + 1] = { "USERS shows a list with all the connected users\n",
                                        "LIST shows a list with all the items for sale\n",
                                        "KICK <username (3-24 characters)> kicks the user named <username>\n",
                                        "PROM shows a list with all the active promoters\n",
                                        "REPROM reloads promoters\n",
                                        "CANCEL <promoter-name (1-24 characters)> stops promoter named <promoter-name>\n",
                                        "CLOSE closes the whole backend and frontends\n",
                                        "HELP shows all commands info\n" };

// parseInstruction
// this function will parse the given command line
//
bool parseInstruction( char* ins_buffer, size_t buffer_size, PINS_INFO pout )
{
	// sanity checks
	if ( !pout || buffer_size < 1 || strlen( ins_buffer ) < 1 )
		return false;
	
	// duping original buffer to don't mess with it
	char* temp_ins_buffer = ( char* ) calloc( 1, buffer_size );
	if ( !temp_ins_buffer )
		return false;
	
	strcpy( temp_ins_buffer, ins_buffer );
	
	// parsing string
	char* ins_type = strtok( temp_ins_buffer, " " );
	if ( !ins_type )
		return false;
	
	// lowercase the first command
	for ( char* p = ins_type; *p; ++p ) *p = ( char ) tolower( *p );
	
	// checking in original string
	char* ins_params = ins_type + strlen( ins_type ) + ( strlen( ins_buffer ) > strlen( ins_type ) ? 1 : 0 );
	
	// filling out struct
	bool status = true;
	
	if ( !strcmp( ins_type, "users" ) )
	{
		pout->type = USERS;
		
		if ( strlen( ins_params ) )
			status = false;
	}
	else if ( !strcmp( ins_type, "list" ) )
	{
		pout->type = LIST;
		
		if ( strlen( ins_params ) )
			status = false;
	}
	else if ( !strcmp( ins_type, "kick" ) )
	{
		pout->type = KICK;
		
		if ( strlen( ins_params ) > 24 || strlen( ins_params ) < 3 )
			status = false;
		else
			memcpy( pout->buffer, ins_params, strlen( ins_params ) + 1 );
	}
	else if ( !strcmp( ins_type, "prom" ) )
	{
		pout->type = PROM;
		
		if ( strlen( ins_params ) )
			status = false;
	}
	else if ( !strcmp( ins_type, "reprom" ) )
	{
		pout->type = REPROM;
		
		if ( strlen( ins_params ) )
			status = false;
	}
	else if ( !strcmp( ins_type, "cancel" ) )
	{
		pout->type = CANCEL;
		
		if ( strlen( ins_params ) > 24 || strlen( ins_params ) < 1 )
			status = false;
		else
			memcpy( pout->buffer, ins_params, strlen( ins_params ) + 1 );
	}
	else if ( !strcmp( ins_type, "close" ) )
	{
		pout->type = CLOSE;
		
		if ( strlen( ins_params ) )
			status = false;
	}
	else if ( !strcmp( ins_type, "help" ) )
	{
		pout->type = HELP;
		
		if ( strlen( ins_params ) )
			status = false;
	}
	else
	{
		// releasing resources
		free( temp_ins_buffer );
		
		printf( "\nInvalid command\n\n" );
		
		return false;
	}
	
	// releasing resources
	free( temp_ins_buffer );
	
	// show how to use the attempted instruction in case the type exists
	if ( !status )
		showInstructionSyntax( pout->type );
	
	return status;
}

// showInstructionSyntax
// this function will show the proper way to call the given instruction
//
void showInstructionSyntax( INS_TYPE type )
{
	printf( "Bad usage of %s\n\n%s\n", instruction_types[ type ], help_messages[ type ] );
}
