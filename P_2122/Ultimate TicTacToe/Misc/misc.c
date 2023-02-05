#include <ctype.h>
#include <string.h>
#include <stdbool.h>

#include "misc.h"

int istrcmp( char* a, char* b )
{
	for ( int i = 0; a[ i ]; i++ )
	{
		int diff = tolower( a [i] ) - tolower( b [i] );
		if ( diff )
			return diff;
	}
	return 0;
}

// isValidFileName
// this function will verify if the filename provided at buffer is a valid name ( letters, numbers, spaces, and ( ) _ - , .   )
//
bool isValidFileName( char* buffer )
{
	const char valid_schars [] = "()_-,.";
	const unsigned int buffer_size = strlen( buffer );
	for ( unsigned int i = 0; i < buffer_size; i++ )
	{
		const char target_char = buffer [i];
		if ( ( target_char >= '0' && target_char <= '9' ) ||
			 ( target_char >= 'A' && target_char <= 'Z' ) ||
			 ( target_char >= 'a' && target_char <= 'z' ) )
			continue;

		for ( unsigned int j = 0; j < sizeof( valid_schars ) - 1; j++ )
			if ( target_char == valid_schars [j] )
				continue;

		return false;
	}

	return true;
}