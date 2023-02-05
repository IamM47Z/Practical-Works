#ifndef MISTERYFOOD_H
#define MISTERYFOOD_H

#include "food.h"

class MisteryFood : public Food
{
public:
	MisteryFood( );
	
	void processTick( ) override;
	
	std::shared_ptr< Entity > getClone( ) override;
};

#endif //MISTERYFOOD_H