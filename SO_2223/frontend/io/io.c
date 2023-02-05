#include <fcntl.h>
#include <string.h>
#include <unistd.h>

#include "io.h"

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

bool toggleIoBlockable( PIO pio )
{
	if ( !pio || !pio->initialized )
		return false;
	
	return fcntl( pio->fd, F_SETFL, fcntl( pio->fd, F_GETFL ) | O_NONBLOCK ) != -1;
}