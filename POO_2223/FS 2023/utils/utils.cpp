#include <random>

#include "utils.h"

namespace utils
{
	COORD_PAIR clampCoords( COORD_PAIR max_coords, COORD_PAIR coords )
	{
		coords.first = ( ( coords.first - 1 ) % max_coords.first ) + 1;
		coords.second = ( ( coords.second - 1 ) % max_coords.second ) + 1;
		
		if ( coords.first <= 0 )
			coords.first = max_coords.first + coords.first;
		
		if ( coords.second <= 0 )
			coords.second = max_coords.second + coords.second;
		
		return coords;
	}
	
	int genRandomNum( int min, int max )
	{
		static std::random_device dev;
		static std::minstd_rand num_gen( dev( ) );
		
		return static_cast<int>( num_gen( ) ) % ( max - min + 1 ) + min;
	}
	
	bool istrcmp( const std::string& string1, const std::string& string2 )
	{
		if ( string1.length( ) != string2.length( ) )
			return false;
		
		for ( auto i = 0; i < string1.length( ); i++ )
			if ( tolower( string1[ i ] ) != tolower( string2[ i ] ) )
				return false;
		
		return true;
	}
	
	DIRECTION getTargetDirection( COORD_PAIR head, COORD_PAIR target, bool invert )
	{
		const auto diff_x = target.first - head.first;
		const auto diff_y = target.second - head.second;
		
		DIRECTION dir{ };
		
		if ( diff_x )
			dir.value += ( ( diff_x > 0 ) ? ( invert ? 0b100 : 0b1000 ) : ( invert ? 0b1000 : 0b100 ) );
		
		if ( diff_y )
			dir.value += ( ( diff_y > 0 ) ? ( invert ? 0b01 : 0b10 ) : ( invert ? 0b10 : 0b01 ) );
		
		return dir;
	}
}