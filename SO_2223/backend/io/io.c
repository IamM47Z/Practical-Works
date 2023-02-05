#include <fcntl.h>
#include <stdlib.h>
#include <unistd.h>
#include <stdbool.h>
#include <sys/stat.h>
#include <string.h>
#include <errno.h>

#include "io.h"

int readFile( char* path, char** pbuffer )
{
	if ( !path || !pbuffer )
		return -1;
	
	// check if file is readable and get file size
	struct stat fs;
	if ( access( path, R_OK ) != 0 || stat( path, &fs ) != 0 )
		return -1;
	
	// allocate memory
	char* buffer = calloc( 1, fs.st_size );
	if ( !buffer )
		return -1;
	
	// open file
	int fd = open( path, O_RDONLY );
	if ( fd == -1 )
	{
		free( buffer );
		return -1;
	}
	
	// read file
	int bytes_read = ( int ) read( fd, buffer, fs.st_size );
	if ( bytes_read == -1 )
		free( buffer );
	else
		*pbuffer = buffer;
	
	close( fd );
	
	return bytes_read;
}

bool isValidFile( char* path, int mode )
{
	if ( !path )
		return false;
	
	return access( path, mode ) == 0;
}

bool initializeIo( PIO pio, char* pathname, int flags )
{
	if ( !pio || pio->initialized || !pathname )
		return false;
	
	int fd = open( pathname, flags );
	if ( fd == -1 )
		return false;
	
	pio->fd = fd;
	pio->initialized = true;
	
	strcpy( pio->pathname, pathname );
	
	return true;
}

void destroyIo( PIO pio )
{
	if ( !pio || !pio->initialized )
		return;
	
	close( pio->fd );
	
	pio->initialized = false;
}

int readIoFifo( PIO pio, char* buffer, size_t size )
{
	if ( !pio || !buffer )
		return -1;
	
	return ( int ) read( pio->fd, buffer, size );
}

int writeIoFifo( PIO pio, char* buffer, size_t size )
{
	if ( !pio || !buffer )
		return -1;
	
	return ( int ) write( pio->fd, buffer, size );
}