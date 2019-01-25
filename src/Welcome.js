import React, { Component } from 'react';
import './Welcome.css';
import firebase from "./Firestore.js";
import Card from '@material-ui/core/Card';
import CardActions from '@material-ui/core/CardActions';
import Button from '@material-ui/core/Button';
import Typography from '@material-ui/core/Typography';
import TextField from '@material-ui/core/TextField';
import { CardContent } from '@material-ui/core';


class Welcome extends Component {

    constructor() {
        super();
        this.state = {
            email: "failed",
            password: "failed"
        }
    }

    componentDidMount() {

    }

    attemptLogin() {
        this.props.setLoading(true);
        firebase.auth().signInWithEmailAndPassword(this.state.email, this.state.password).catch(function (error) {
            // Handle Errors here.
            /*
            var errorCode = error.code;
            var errorMessage = error.message;
            */
            // ...
        });
        console.log(this.state.email + " / " + this.state.password)
    }

    render() {
        return (
            <div className="Welcome">
                <Card className="Welcome--CardLogin">
                    <CardContent>
                    <Typography color="textSecondary" gutterBottom>
                        Naneos - Analyze
                </Typography>
                    <Typography variant="h5" component="h2">
                        Welcome
                </Typography>
               

                    <TextField className="Welcome--TextField"
                        label="E-Mail"
                        onChange={(e) => this.setState({ email: e.target.value })}
                        margin="normal"
                    />

                    <TextField className="Welcome--TextField"
                        label="Password"
                        onChange={(x) => this.setState({ password: x.target.value })}
                        margin="normal"
                    />
                     </CardContent>


                    <CardActions className="Welcome--Button">
                        <Button  onClick={() => this.attemptLogin()}>Login</Button>
                    </CardActions>
                </Card>                   
            </div>
        )
    }
}



export default Welcome;