#ifndef FOOD_H
#define FOOD_H

#include <string>
#include <vector>
#include <utility>

#include "../entity.h"
#include "../../shared.h"

class FoodEntry
{
protected:
	std::string species{ };
	int nutritive_points{ }, toxicity_points{ };

public:
	std::string getSpecies( )
	{
		return species;
	}
	
	[[nodiscard]] int getToxicity( ) const
	{
		return toxicity_points;
	}
	
	[[nodiscard]] int getNutritive( ) const
	{
		return nutritive_points;
	}
	
	FoodEntry( std::string species, int nutritive_points, int toxicity_points ) :
			species( std::move( species ) ), nutritive_points( nutritive_points ),
			toxicity_points( toxicity_points ) { }
	
	FoodEntry( ) = default;
};

class FoodHistory
{
	class FoodNode
	{
	public:
		FoodEntry entry;
		FoodNode* pnext;
		
		explicit FoodNode( FoodEntry& entry )
		{
			this->entry = entry;
			pnext = nullptr;
		}
	};
	
	FoodNode* phead;
	FoodNode* pcur;
public:
	
	void push( FoodEntry entry )
	{
		auto* pentry = new FoodNode( entry );
		if ( !pentry )
		{
			endwin( );
			perror( "Error allocating a Node." );
			exit( 1 );
		}
		
		pentry->pnext = this->phead;
		this->phead = pentry;
		this->pcur = this->phead;
	}
	
	FoodEntry* get( )
	{
		if ( !this->pcur )
			this->pcur = this->phead;
		
		if ( this->pcur )
			return &this->pcur->entry;
		
		return nullptr;
	}
	
	void next( )
	{
		if ( this->pcur )
			this->pcur = this->pcur->pnext;
	}
	
	bool hasNext( )
	{
		return this->pcur != nullptr;
	}
	
	void reset( )
	{
		this->pcur = this->phead;
	}
};

class Food : public FoodEntry, public Entity
{
protected:
	// information
	//
	int age = 0, duration{ };
	std::vector< SMELL > smells;
	
	bool addSmell( SMELL smell );

public:
	explicit Food( const std::string& species );
	
	FoodEntry& getEntry( );
	
	bool smellsLike( SMELL smell );
	
	bool moveTo( COORD_PAIR new_coords ) override;
};

typedef std::shared_ptr< Food > FoodPtr;

#endif //FOOD_H
