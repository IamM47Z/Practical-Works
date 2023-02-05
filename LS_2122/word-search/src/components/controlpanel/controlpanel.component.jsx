import React from 'react';
import './controlpanel.css';

class ControlPanel extends React.Component
{
    getBtnStr( )
    {
        return ( this.props.game_started ? "Stop" : "Start" );
    }

    render( ) 
    {
        return (
            <div className='control-panel'>
                <h4>Settings:</h4>
                <div className="btn-group">
                    <button className="btn btn-secondary btn-sm dropdown-toggle" type="button" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                        Difficulty ( Current: { this.props.level_str } )
                    </button>
                    <div className="dropdown-menu">
                        <a className="dropdown-item" href="/#" onClick={ () => this.props.changeLevel( 0 ) }>Easy</a>
                        <a className="dropdown-item" href="/#" onClick={ () => this.props.changeLevel( 1 ) }>Medium</a>
                        <a className="dropdown-item" href="/#" onClick={ () => this.props.changeLevel( 2 ) }>Hard</a>
                    </div>
                </div>
                <div className='btn-group'>
                    <label>Word: </label>
                    <input type="text" id="new_word"></input>
                </div>
                <div id="word_list" className='btn-group'>
                    { 
                        ( 
                            <div className="add-words-group">
                                {
                                    this.props.words.map( ( word ) => {
                                        return (
                                            <a href="\#" onClick={ ( ) => {
                                                document.getElementById( "new_word" ).value = word;
                                                this.props.removeWord( );
                                            } } >{ word }</a>
                                        );
                                    })
                                } 
                            </div>
                        )
                    }
                </div>
                <div className='btn-group'>
                    <button className="btn btn-secondary btn-sm" type="button" onClick={ this.props.addWord }>Add</button>
                    <button className="btn btn-secondary btn-sm" type="button" onClick={ this.props.removeWord }>Remove</button> 
                </div>
                <div className='btn-group btn-game-state'>
                    <button  className="btn btn-primary btn-sm" type="button" onClick={ this.props.toggleGameState }> { this.getBtnStr( ) } </button>
                </div>
            </div>
        );
    }
}

export default ControlPanel;