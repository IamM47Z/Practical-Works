#include "corpses.h"

#include "../../consts/consts.h"

Corpses::Corpses( int nutritive_points, int toxicity_points ) : Food( "Corpses" )
{
	visual_representation = 'p';
	
	duration = -1;
	this->toxicity_points = toxicity_points;
	this->nutritive_points = nutritive_points;
	
	if ( !addSmell( SMELL::CARNE ) )
	{
		endwin( );
		perror( "Fatal error initializing Corpses Smells" );
		exit( 1 );
	}
}

void Corpses::processTick( )
{
	age++;
	nutritive_points--;
	
	if ( age < ( 2 * ( age + nutritive_points ) ) )
		toxicity_points++;
}

std::shared_ptr< Entity > Corpses::getClone( )
{
	auto pclone = std::make_shared< Corpses >( nutritive_points, toxicity_points );
	
	// entity class
	//
	pclone->id = id;
	pclone->alive = alive;
	
	pclone->range = range;
	pclone->visual_representation = visual_representation;
	pclone->coords = coords;
	
	// foodentry class
	//
	pclone->species = species;
	pclone->nutritive_points = nutritive_points;
	pclone->toxicity_points = toxicity_points;
	
	// food class
	//
	pclone->age = age;
	pclone->duration = duration;
	pclone->smells = smells;
	
	return pclone;
}