#include "engine/engine.h"

int main( )
{
	if ( !engine::initialize( ) )
		return 1;
	
	while ( true )
	{
		engine::render( );
		
		if ( !engine::processKeys( ) )
			break;
	}
	
	return 0;
}