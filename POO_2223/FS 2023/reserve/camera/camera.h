#ifndef CAMERA_H
#define CAMERA_H

#include <map>
#include <vector>
#include <utility>
#include <csignal>
#include <sys/ioctl.h>

#include "../../shared.h"

#include "../../terminal/terminal.h"

class Camera
{
	// world information
	//
	COORD_PAIR cur_tlcorner;    // top left corner
	COORD_PAIR field_measures;
	std::map< COORD_PAIR, std::string > ui_buffer;
	
	COORD_PAIR world_viewport;
	
	// dimensions
	//
	std::pair< int, int > terminal_viewport, square_viewport, term_tlcorner;

public:
	Camera( int field_width, int field_height );
	
	[[nodiscard]] bool isVisibleArea( COORD_PAIR location ) const;
	
	bool updateArea( COORD_PAIR location, char representation );
	
	void clearBuffer( );
	
	void render( );
	
	void reset( );
	
	[[nodiscard]] term::Window& getConsoleHwnd( ) const;
	
	[[nodiscard]] term::Window& getReserveHwnd( ) const;
	
	bool moveVisibleArea( DIRECTION direction, int steps );
	
	bool processKeys( const std::function< bool( term::Window& hwnd, const std::string& str ) >& on_input_callback );
};

#endif //CAMERA_H
