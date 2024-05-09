import React from 'react';

import Container from 'react-bootstrap/Container';
import {Tab, Tabs} from "react-bootstrap";
import Home from "./home/home";
import Generate from "./generate/Generate";
import Rules from "./rules/Rules";
import { Characters } from './models/Characters';
import Character from './models/Character';
import LivePlay from './liveplay/LivePlay';
import { Log, LogGetter } from './models/Log';


const App: React.FC<{}> = ({}) =>  {

    const [selectedCharacter, setCharacter]: any = React.useState(null as Character | null);
    const [characters]: any = React.useState(new Characters(setCharacter));

    const [log, setLog]: any = React.useState(new Log());
    let logGetter: LogGetter = new LogGetter(setLog);

    return (
        <Container className="p-3">
            <Container>
                <h2 className="header">Forgetful RPG Assistant</h2>
            </Container>
            <hr/>
            <div className="mh-100">
                <Tabs className="mb-3" justify>
                    <Tab eventKey="home" title="Home">
                        <Home characters={characters} logGetter={logGetter} log={log} />
                    </Tab>
                    <Tab eventKey="generate" title="Generate">
                        <Generate characters={characters} />
                    </Tab>
                    <Tab eventKey="rules" title="Rules">
                        <Rules/>
                    </Tab>
                    <Tab eventKey="livePlay" title="LivePlay">
                        <LivePlay characters={characters} logGetter={logGetter} />
                    </Tab>
                </Tabs>
            </div>
        </Container>
    );
};

export default App;
