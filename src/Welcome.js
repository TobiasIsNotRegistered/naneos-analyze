import React, { Component } from 'react';
import './Welcome.css';
import firebase from "./Firestore.js";


class Welcome extends Component {

    constructor(){
        super();
        this.state = {
            email: "failed",
            password: "failed"
        }
    }

    componentDidMount(){
       
    }

    attemptLogin(){
        this.props.setLoading;
        firebase.auth().signInWithEmailAndPassword(this.state.email, this.state.password).catch(function(error) {
            // Handle Errors here.
            var errorCode = error.code;
            var errorMessage = error.message;
            // ...
          });
        console.log(this.state.email + " / " + this.state.password)
    }

    render(){

        return(
        <div className="Welcome">
        <h1> Naneos-Analyze</h1>
   
   
        <div className="login">
           <p>email:</p>
           <input onChange={(e) => this.setState({email: e.target.value})}></input>
           <p>password:</p>
           <input onChange={(x) => this.setState({password: x.target.value})}></input>
           <button onClick={() => this.attemptLogin()}>Login!</button>
        </div>
       </div>
        )
    }
}

   

export default Welcome;