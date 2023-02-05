#ifndef CORPSES_H
#define CORPSES_H

#include "food.h"

class Corpses : public Food
{
public:
	Corpses( int nutritive_points, int toxicity_points );
	
	void processTick( ) override;
	
	std::shared_ptr< Entity > getClone( ) override;
};

#endif //CORPSES_H