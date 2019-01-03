import React, { Component } from 'react';
import firebase from "./Firestore.js";
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, Brush, ReferenceLine } from 'recharts';
import Clock from "./Clock.js";


class ChartContainer extends Component {

    constructor() {
        super();
        this.state = {

        }

    }

    componentDidMount() {

    }



    render() {
        let current_chart_data = this.props.data;
        this.dataStore = this.props.dataStore;

        if (this.dataStore.length > 0) {
            return (
                <div className="container">
                    <h1>I am a ChartContainer</h1>
                    <button onClick={() => this.deleteAllElementsInFirestore()}>Delete!</button>

                    <div className="App-intro">
                        <p>Amount of local data: {this.dataStore.length}</p>
                        <p>Amount of data in chart-array: {current_chart_data.length}</p>
                        <Clock youngestEntry={this.dataStore[0].date} ></Clock>
                        <p>Youngest Entry: {this.dataStore[0].date.toLocaleString()}</p>
                        <p>Oldest Entry: {this.dataStore[this.dataStore.length - 1].date.toLocaleString()}</p>
                    </div>

                    <LineChart width={0.9 * this.state.width} height={300} data={current_chart_data} margin={{ top: 5, right: 30, left: 20, bottom: 5 }} syncId="main_sync">
    <XAxis dataKey="dateShort" />
    <YAxis />
    <CartesianGrid strokeDasharray="3 3" />
    <Tooltip />
    <Legend />
    <Brush></Brush>
    <Line type="monotone" dataKey={this.state.currentDataKey1} stroke="#8884d8" activeDot={{ r: 8 }} connectNulls={true}/>
  </LineChart>

  <LineChart width={0.9 * this.state.width} height={300} data={current_chart_data} margin={{ top: 5, right: 30, left: 20, bottom: 5 }} syncId="main_sync">
    <XAxis dataKey="dateShort" />
    <YAxis />
    <CartesianGrid strokeDasharray="3 3" />
    <Tooltip />§
    <Legend />
    <Line type="monotone" dataKey={this.state.currentDataKey2} stroke="#8884d8" activeDot={{ r: 8 }} connectNulls={true} />
  </LineChart>



  <button onClick={() => this.addElementsToDataArray()}>Add!</button>
  <button onClick={() => this.removeElementsToDataArray()}>remove!</button>

  <div className="mainContent">
    {this.dataStore
      .map(e => {
        return <p>{e.dateLong} //</p>
      })}
  </div>



                </div>
            )}else{
                return(<p>No Data available?!</p>);                
            }
        }
    
}



export default ChartContainer;




