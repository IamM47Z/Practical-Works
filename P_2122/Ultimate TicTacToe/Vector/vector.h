#pragma once

typedef struct _VECTOR
{
	void* buffer;
	int num_elems; 
	int elem_size;
} VECTOR, *PVECTOR;

void deleteVec( PVECTOR pvec );
PVECTOR initializeVec( int elem_size, int num_elems );
bool resizeVec( PVECTOR pvec, int new_num_elems );