#ifndef ANIMAL_H
#define ANIMAL_H

#include <string>
#include <vector>

#include "../../shared.h"
#include "../foods/food.h"

class Animal : public Entity
{
protected:
	int parent_id = 0;
	
	// information
	//
	std::string species{ };
	
	int age{ };
	int est_age{ };                         // age where the animalspecies will most likely die
	std::vector< int > childs_id{ };
	
	int health{ };
	int hunger{ };
	float weight{ };
	
	FoodHistory history{ };                 // using linked list since we cannot use STL Containers
	std::vector< SMELL > taste{ };

public:
	explicit Animal( std::string species );
	
	std::string getSpecies( );
	
	[[nodiscard]] int getHealth( ) const;
	
	[[nodiscard]] float getWeight( ) const;
	
	void eat( const FoodEntry& entry );
	
	bool addChild( int child );
	
	// enforcing LSP design principle (one of SOLID object-oriented design principles)
	//
	virtual void reproduce( ) { }
	
	virtual void move( DIRECTION direction ) = 0;
};

typedef std::shared_ptr< Animal > AnimalPtr;

#endif //ANIMAL_H
