import React from 'react';
import './wordspanel.css';

class WordsPanel extends React.Component
{
    renderWordsPanel( )
    {
        if ( !this.props.words_array.length )
            return ( <div>You need to start a new Match!</div> );

        return this.props.words_array.map( ( word ) =>
        {
            return ( <div className={ word.class }> { word.word } </div> );
        });
    }

    render( ) 
    {
        return ( 
            <div className='words-panel'>
                <h4>Words ( Remaining { this.props.num_unspoted_words } ):</h4>
                { this.renderWordsPanel( ) }
            </div>
        );
    }
}

export default WordsPanel;