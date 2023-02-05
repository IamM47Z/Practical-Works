#include <sstream>

#include "camera.h"

Camera::Camera( int field_width, int field_height )
{
	cur_tlcorner = std::make_pair( 1, 1 );
	field_measures = std::make_pair( field_width, field_height );
	
	// simulation viewport in simulation distance
	//
	world_viewport = std::make_pair( static_cast<int>( 1.0f / TERM_SQUARE_WIDTH ),
	                                 static_cast<int>( 1.0f / TERM_SQUARE_HEIGHT ) );
	
	// terminal viewport
	//
	terminal_viewport = std::make_pair(
			static_cast<int>( static_cast<float>(term::Terminal::getNumCols( )) * TERM_RESERVE_WIDTH ),
			static_cast<int>( static_cast<float>(term::Terminal::getNumRows( )) * TERM_RESERVE_HEIGHT ) );
	
	// square viewport
	//
	square_viewport = std::make_pair(
			static_cast<int>( static_cast<float>(terminal_viewport.first) * TERM_SQUARE_WIDTH ),
			static_cast<int>( static_cast<float>(terminal_viewport.second) * TERM_SQUARE_HEIGHT ) );
	
	// terminal draw corner
	//
	term_tlcorner = std::make_pair(
			static_cast<int>( static_cast<float>(term::Terminal::getNumCols( )) * TERM_RESERVE_X ),
			static_cast<int>( static_cast<float>(term::Terminal::getNumRows( )) * TERM_RESERVE_Y ) );
}

bool Camera::isVisibleArea( COORD_PAIR location ) const
{
	return ( cur_tlcorner.first <= location.first && cur_tlcorner.first + world_viewport.first >= location.first ) &&
	       ( cur_tlcorner.second <= location.second && cur_tlcorner.second + world_viewport.second >= location.second );
}

bool Camera::updateArea( COORD_PAIR location, char representation )
{
	if ( !isVisibleArea( location ) )
		return false;
	
	if ( ui_buffer.find( location ) == ui_buffer.cend( ) )
		ui_buffer[ location ] = std::string( 1, representation );
	else
		ui_buffer[ location ] += ", " + std::string( 1, representation );
	
	return true;
}

void Camera::clearBuffer( )
{
	ui_buffer.clear( );
}

void Camera::render( )
{
	auto& reserve_hwnd = getReserveHwnd( );
	
	static const auto main_hwnd = term::Terminal::create_window( term_tlcorner.first, term_tlcorner.second,
	                                                             square_viewport.first, square_viewport.second, true );
	
	// terminal pos
	//
	auto cpos = std::move( term_tlcorner );
	
	// simulation pos
	//
	auto cloc = std::move( cur_tlcorner );
	
	// iterate all terminal positions
	//
	while ( cloc.second <= cur_tlcorner.second + world_viewport.second )
	{
		auto sub_hwnd = term::Terminal::create_window(
				cpos.first, cpos.second, square_viewport.first, square_viewport.second, false );
		
		std::ostringstream buffer;
		buffer << cloc.first << "x" << cloc.second;
		sub_hwnd << term::move_to(
				( square_viewport.first / 2 ) - ( static_cast<int>(buffer.str( ).length( )) / 2 ),
				0 ) << buffer.str( );
		
		if ( ui_buffer.find( cloc ) != ui_buffer.cend( ) )
			sub_hwnd << term::move_to(
					( square_viewport.first / 2 ) - ( static_cast<int>(ui_buffer[ cloc ].length( )) / 2 ), 1 )
			         << ui_buffer[ cloc ];
		
		cloc.first++;
		cpos.first += square_viewport.first;
		
		if ( cloc.first > cur_tlcorner.first + world_viewport.first )
		{
			cloc.first = cur_tlcorner.first;
			cloc.second++;
			
			cpos.first = term_tlcorner.first;
			cpos.second += square_viewport.second;
		}
	}
}

void Camera::reset( )
{
	endwin( );
	refresh( );
	clear( );
	flushinp( );
	
	auto& terminal = term::Terminal::instance( );
	terminal_viewport = std::make_pair(
			static_cast<int>(static_cast<float>(term::Terminal::getNumCols( )) * TERM_RESERVE_WIDTH),
			static_cast<int>(static_cast<float>(term::Terminal::getNumRows( )) * TERM_RESERVE_HEIGHT) );
	
	term_tlcorner = std::make_pair(
			static_cast<int>(static_cast<float>(term::Terminal::getNumCols( )) * TERM_RESERVE_X),
			static_cast<int>(static_cast<float>(term::Terminal::getNumRows( )) * TERM_RESERVE_Y) );
	
	square_viewport = std::make_pair(
			static_cast<int>(static_cast<float>(terminal_viewport.first) * TERM_SQUARE_WIDTH),
			static_cast<int>(static_cast<float>(terminal_viewport.second) * TERM_SQUARE_HEIGHT) );
	
	auto& console_hwnd = getConsoleHwnd( );
	console_hwnd = term::Terminal::create_window( term_tlcorner.first,
	                                              terminal_viewport.second +
	                                              static_cast<int>(static_cast<float>(term_tlcorner.second) *
	                                                               1.5f ) +
	                                              square_viewport.second,
	                                              terminal_viewport.first, terminal_viewport.second );
	
	auto& reserve_hwnd = getReserveHwnd( );
	reserve_hwnd = term::Terminal::create_window( term_tlcorner.first - 1, term_tlcorner.second - 1,
	                                              square_viewport.first * ( world_viewport.first + 1 ) + 2,
	                                              square_viewport.second * ( world_viewport.second + 1 ) + 2 );
}

bool Camera::processKeys( const std::function< bool( term::Window& hwnd, const std::string& str ) >& on_input_callback )
{
	if ( !on_input_callback )
		return false;
	
	auto& console_hwnd = getConsoleHwnd( );
	
	console_hwnd.move( 0, 0 );
	console_hwnd << "Command: ";
	
	std::string str;
	
	curs_set( 1 );
	console_hwnd >> str;
	curs_set( 0 );
	
	if ( str == "KEY_UP" )
	{
		if ( cur_tlcorner.second > 1 )
			cur_tlcorner.second--;
	}
	else if ( str == "KEY_DOWN" )
	{
		if ( cur_tlcorner.second + world_viewport.second < field_measures.second )
			cur_tlcorner.second++;
	}
	else if ( str == "KEY_LEFT" )
	{
		if ( cur_tlcorner.first > 1 )
			cur_tlcorner.first--;
	}
	else if ( str == "KEY_RIGHT" )
	{
		if ( cur_tlcorner.first + world_viewport.first < field_measures.first )
			cur_tlcorner.first++;
	}
	else if ( str == "KEY_RESIZE" )
		reset( );
	else
		return on_input_callback( console_hwnd, str );
	
	// clear old information from buffer (the information will be updated by the reserve)
	//
	for ( auto it = ui_buffer.cbegin( ); it != ui_buffer.cend( ); )
		if ( !isVisibleArea( it->first ) )
			ui_buffer.erase( it++ );
		else
			it++;
	return true;
}

bool Camera::moveVisibleArea( DIRECTION direction, int steps )
{
	if ( direction.up )
	{
		if ( cur_tlcorner.second - steps < 1 )
			return false;
		
		cur_tlcorner.second -= steps;
	}
	else if ( direction.down )
	{
		if ( cur_tlcorner.second + steps + world_viewport.second > field_measures.second )
			return false;
		
		cur_tlcorner.second += steps;
	}
	else if ( direction.left )
	{
		if ( cur_tlcorner.first - steps < 1 )
			return false;
		
		cur_tlcorner.first -= steps;
	}
	else if ( direction.right )
	{
		if ( cur_tlcorner.first + steps + world_viewport.first > field_measures.first )
			return false;
		
		cur_tlcorner.first += steps;
	}
	else
		return false;
	
	return true;
}

term::Window& Camera::getConsoleHwnd( ) const
{
	// create static console window
	//
	static auto console_hwnd = term::Terminal::create_window(
			term_tlcorner.first,
			terminal_viewport.second + static_cast<int>(static_cast<float>(term_tlcorner.second) * 1.5f ) +
			square_viewport.second,
			terminal_viewport.first, terminal_viewport.second );
	
	return console_hwnd;
}

term::Window& Camera::getReserveHwnd( ) const
{
	// create static reserve hwnd
	//
	static auto reserve_hwnd = term::Terminal::create_window( term_tlcorner.first - 1, term_tlcorner.second - 1,
	                                                          square_viewport.first * ( world_viewport.first + 1 ) + 2,
	                                                          square_viewport.second * ( world_viewport.second + 1 ) +
	                                                          2, true );
	return reserve_hwnd;
}