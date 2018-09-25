import React, { Component } from 'react';
import logo from './logo.svg';
import './App.css';
import firebase from "./Firestore.js";

class App extends Component {

  constructor() {
    super();
    this.state = {
      data: []
    };
  }

  componentDidMount() {
    //this.getDataFromFirestoreOnce();
    this.getDataFromFirestoreContinously();
  }

  getDataFromFirestoreOnce() {
    //make connection
    const db = firebase.firestore();
    const settings = { timestampsInSnapshots: true };
    db.settings(settings);
    var colRef = db.collection("DummyData");
    let temp = this.state.data;

    //retrieve data one by one, store in array 'temp'
    colRef.get().then((snapshot) => {
      snapshot.forEach((doc) => {
        temp.push(doc.data())
      })
    }

      //after retrieving, sort the temp-array
    ).then(() => {
      temp.sort((e1, e2) => { e1.date >= e2.date });
      //only change state via setState
      this.setState({ data: temp });
    }
    )
  }

  getDataFromFirestoreContinously() {
    //make connection
    const db = firebase.firestore();
    const settings = { timestampsInSnapshots: true };
    db.settings(settings);
    var colRef = db.collection("DummyData");
    let temp = this.state.data;

    //listen for changes
    colRef.onSnapshot(snapshot => {
      snapshot.docChanges().forEach(function(change) {
        if (change.type === "added") {           
            temp.push(change.doc.data());
        }
        if (change.type === "modified") {
           
        }
        if (change.type === "removed") {
           
        }
    });      
      //only change state via setState
      this.setState({ data: temp });
    })
  }


  render() {
    return (
      <div className="App">
        <header className="App-header">
          <img src={logo} className="App-logo" alt="logo" />
          <h1 className="App-title">Welcome to React</h1>
        </header>
        <p className="App-intro">
          Entries in DB: {this.state.data.length}
        </p>
        {this.state.data
        .sort((e1, e2) => {return (e1.date.toDate() - e2.date.toDate())})
        .map(e => {
          return (<p>{e.date.toDate().toLocaleString()} / {e.content}</p>)
        })}
      </div>
    );
  }
}

export default App;
