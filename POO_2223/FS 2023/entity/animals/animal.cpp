#include "animal.h"

Animal::Animal( std::string species ) : Entity( )
{
	this->species = std::move( species );
}

std::string Animal::getSpecies( )
{
	return species;
}

int Animal::getHealth( ) const
{
	return health;
}

float Animal::getWeight( ) const
{
	return weight;
}

bool Animal::addChild( int child_id )
{
	if ( std::count( childs_id.begin( ), childs_id.end( ), child_id ) )
		return false;
	
	childs_id.push_back( child_id );
	return true;
}

void Animal::eat( const FoodEntry& entry )
{
	health += entry.getNutritive( ) - entry.getToxicity( );
	
	this->history.push( entry );
}