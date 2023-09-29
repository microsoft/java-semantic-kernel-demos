import React, {useEffect, useState} from 'react';
import FactsPane from "./FactsPane";
import {Col, Row} from "react-bootstrap";
import QuestionPane from "./QuestionPane";
import CharacterPane from "./CharacterPane";
import CharacterLogPane from "./CharacterLogPane";
import LogPane from "./LogPane";
import CharSelect from "./CharSelect";
import Character, {CharacterGetter} from "../models/Character";
import {Log, LogGetter} from "../models/Log";


const Home: React.FC = () => {
    const [character, setCharacter]: any = useState(null as Character | null);
    const [log, setLog]: any = useState(new Log());

    let logGetter: LogGetter = new LogGetter(setLog);
    let loadCharacter: CharacterGetter = new CharacterGetter(setCharacter);

    useEffect(() => {
        logGetter.get();
    }, [])
    return (
        <Row>
            <Col xs={9}>
                <Row className="p-3">
                    <Col xs={6}>
                        <Row className="p-3">
                            <h4>Select Character</h4>
                            <CharSelect loadCharacter={loadCharacter}/>
                        </Row>
                        <Row className="p-3">
                            <h4>Player Facts</h4>
                            <FactsPane character={character}/>
                        </Row>
                        <Row className="p-3">
                            <Col xs={12}>
                                <h4>Character Stats</h4>
                                <CharacterPane character={character}/>
                            </Col>
                        </Row>
                    </Col>
                    <Col xs={6}>
                        <Row className="p-3">
                            <Col xs={12}>
                                <QuestionPane logGetter={logGetter} character={character}
                                              loadCharacter={loadCharacter}/>
                            </Col>
                        </Row>
                        <Row className="p-3">
                            <Col xs={12}>
                                <h4>Player Log</h4>
                                <CharacterLogPane character={character}/>
                            </Col>
                        </Row>
                    </Col>
                </Row>
            </Col>
            <Col xs={3}>
                <Row className="p-3">
                    <h4>Game Log</h4>
                    <LogPane gameLog={log}/>
                </Row>
            </Col>
        </Row>
    );
};

export default Home;
