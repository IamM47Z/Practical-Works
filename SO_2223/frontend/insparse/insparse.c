#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdbool.h>
#include <ctype.h>

#include "insparse.h"

// all instruction type names
//
const char* instruction_types[] = { "SELL", "LIST", "LICAT", "LISEL", "LIVAL", "LITIME",
                                    "TIME", "BUY", "CASH", "ADD", "EXIT", "HELP" };

// all instruction help messages
//
const char* help_messages[
		HELP + 1] = { "SELL <name> <category> <price> <buy-now-price> <duration> sets an auction for an item\n",
		              "LIST shows a list with all the items for sale\n",
		              "LICAT <category> shows a list with all the items for sale of a specific category\n",
		              "LISEL <username> shows a list with all the items for sale\n",
		              "LIVAL <value> shows a list with all the items for sale\n",
		              "LITIME <time> shows a list with all the items for sale\n",
		              "TIME shows current backend uptime\n",
		              "BUY <id> <value> bids over item with a specified id and value\n",
		              "CASH shows current user balance\n",
		              "ADD <value> deposits a value into current user balance\n",
		              "EXIT closes the current frontend\n",
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
	char* temp_ins_buffer = ( char* ) malloc( buffer_size );
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
	
	if ( !strcmp( ins_type, "sell" ) )
	{
		pout->type = SELL;
		
		status = false;
		
		do
		{
			char* word = strtok( ins_params, " " );
			if ( !word )
				break;
			
			strcpy( pout->name, word );
			
			word = strtok( NULL, " " );
			if ( !word )
				break;
			
			strcpy( pout->category, word );
			
			word = strtok( NULL, " " );
			if ( !word )
				break;
			
			int price = ( int ) strtol( word, NULL, 10 );
			if ( price < 1 )
				break;
			
			pout->price = price;
			
			word = strtok( NULL, " " );
			if ( !word )
				break;
			
			int buy_now_price = ( int ) strtol( word, NULL, 10 );
			if ( buy_now_price < 1 )
				break;
			
			pout->buy_now_price = buy_now_price;
			
			word = strtok( NULL, " " );
			if ( !word )
				break;
			
			int duration = ( int ) strtol( word, NULL, 10 );
			if ( duration < 1 )
				break;
			
			pout->duration = duration;
			
			if ( strtok( NULL, " " ) )
				break;
			
			status = true;
		}
		while ( false );
	}
	else if ( !strcmp( ins_type, "list" ) )
	{
		pout->type = LIST;
		
		if ( strlen( ins_params ) )
			status = false;
	}
	else if ( !strcmp( ins_type, "licat" ) )
	{
		pout->type = LICAT;
		
		if ( strchr( ins_params, ' ' ) )
			status = false;
		else
			strcpy( pout->buffer, ins_params );
	}
	else if ( !strcmp( ins_type, "lisel" ) )
	{
		pout->type = LISEL;
		
		if ( strchr( ins_params, ' ' ) )
			status = false;
		else
			strcpy( pout->buffer, ins_params );
	}
	else if ( !strcmp( ins_type, "lival" ) )
	{
		pout->type = LIVAL;
		
		if ( strchr( ins_params, ' ' ) )
			status = false;
		else
		{
			int value = ( int ) strtol( ins_params, NULL, 10 );
			if ( value > 0 )
				pout->int_value = value;
			else
				status = false;
		}
	}
	else if ( !strcmp( ins_type, "litime" ) )
	{
		pout->type = LITIME;
		
		if ( strchr( ins_params, ' ' ) )
			status = false;
		else
		{
			int value = ( int ) strtol( ins_params, NULL, 10 );
			if ( value > 0 )
				pout->int_value = value;
			else
				status = false;
		}
	}
	else if ( !strcmp( ins_type, "time" ) )
	{
		pout->type = TIME;
		
		if ( strlen( ins_params ) )
			status = false;
	}
	else if ( !strcmp( ins_type, "buy" ) )
	{
		pout->type = BUY;
		
		status = false;
		
		do
		{
			char* word = strtok( ins_params, " " );
			if ( !word )
				break;
			
			int id = ( int ) strtol( word, NULL, 10 );
			if ( id < 0 )
				break;
			
			pout->id = id;
			
			word = strtok( NULL, " " );
			if ( !word )
				break;
			
			int value = ( int ) strtol( word, NULL, 10 );
			if ( value < 1 )
				break;
			
			pout->bid_value = value;
			
			if ( strtok( NULL, " " ) )
				break;
			
			status = true;
		}
		while ( false );
	}
	else if ( !strcmp( ins_type, "cash" ) )
	{
		pout->type = CASH;
		
		if ( strlen( ins_params ) )
			status = false;
	}
	else if ( !strcmp( ins_type, "add" ) )
	{
		pout->type = ADD;
		
		if ( strchr( ins_params, ' ' ) )
			status = false;
		else
		{
			int value = ( int ) strtol( ins_params, NULL, 10 );
			if ( value > 0 )
				pout->int_value = value;
			else
				status = false;
		}
	}
	else if ( !strcmp( ins_type, "exit" ) )
	{
		pout->type = EXIT;
		
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
	printf( "\nBad usage of %s\n\n%s", instruction_types[ type ], help_messages[ type ] );
}
