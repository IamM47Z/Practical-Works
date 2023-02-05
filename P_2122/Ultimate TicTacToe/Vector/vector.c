#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdbool.h>

#include "vector.h"

void deleteVec( PVECTOR pvec )
{
	if ( pvec->buffer )
		free( pvec->buffer );

	if ( pvec )
		free( pvec );
}

PVECTOR initializeVec( int elem_size, int num_elems )
{
	if ( elem_size < 1 )
		return NULL;

	PVECTOR vec = ( PVECTOR )malloc( sizeof( VECTOR ) );
	if ( !vec )
		return NULL;

	vec->num_elems = num_elems;
	vec->elem_size = elem_size;
	vec->buffer = malloc( elem_size * num_elems );
	if ( !vec->buffer )
		return NULL;

	memset( vec->buffer, 0, elem_size * num_elems );

	return vec;
}

bool resizeVec( PVECTOR pvec, int new_num_elems )
{
	if ( new_num_elems < 1 || !pvec || !pvec->buffer )
		return false;

	const int old_size = pvec->elem_size * pvec->num_elems;
	const int delta_size = pvec->elem_size * ( new_num_elems - pvec->num_elems );

	pvec->num_elems = new_num_elems;

	void* new_buffer = realloc( pvec->buffer, pvec->elem_size * new_num_elems );
	if ( !new_buffer )
		return false;

	pvec->buffer = new_buffer;

	if ( delta_size > 0 )
		memset( ( char* )pvec->buffer + old_size, 0, delta_size );

	return true;
}