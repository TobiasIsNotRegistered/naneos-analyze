import React, { Component } from 'react';

class Clock extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            time: new Date().toLocaleString(),
            date: new Date()
        };

       
    }
    componentDidMount() {
        this.intervalID = setInterval(
            () => this.tick(),
            1000
        );
    }
    componentWillUnmount() {
        clearInterval(this.intervalID);
    }
    tick() {
        const x = new Date();
        this.setState({
            time: x.toLocaleString(),
            date: x,
        });
    }

    dateDiffInDays(a, b) {
        
        const MILISECONDS = (a-b);
        const SECONDS = (a-b)/(1000);
        const hours = (a-b)/(1000*60*60);
        const days = (a-b)/(1000*60*60*24);

              

        return new Date(SECONDS * 1000).toISOString().substr(11, 8); ;
    }

    render() {
        return (
            <p className="App-clock">
                The time is {this.state.time} /// 
          deltaT since last update: {this.dateDiffInDays(this.state.date, this.props.youngestEntry)}
            </p>
        );
    }
}

export default Clock;