#ifndef UTILS_H
#define UTILS_H

#include "../shared.h"

namespace utils
{
	COORD_PAIR clampCoords( COORD_PAIR max_coords, COORD_PAIR coords );
	
	int genRandomNum( int min, int max );
	
	bool istrcmp( const std::string& string1, const std::string& string2 );
	
	DIRECTION getTargetDirection( COORD_PAIR head, COORD_PAIR target, bool invert = false );
}


#endif //UTILS_H
