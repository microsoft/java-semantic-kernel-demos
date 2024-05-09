import React, {useEffect, useState} from 'react';
import FactsPane from "./FactsPane";
import {Col, Row} from "react-bootstrap";
import QuestionPane from "./QuestionPane";
import CharacterPane from "./CharacterPane";
import CharacterLogPane from "./CharacterLogPane";
import LogPane from "./LogPane";
import CharSelect from "./CharSelect";
import {Log, LogGetter} from "../models/Log";
import { Characters } from '../models/Characters';


const Home: React.FC<{
    characters: Characters,
    logGetter: LogGetter,
    log: Log
}> = ({
    characters,
    logGetter,
    log
}) => {

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
                            <CharSelect characters={characters}/>
                        </Row>
                        <Row className="p-3">
                            <h4>Player Facts</h4>
                            <FactsPane characters={characters}/>
                        </Row>
                        <Row className="p-3">
                            <Col xs={12}>
                                <h4>Character Stats</h4>
                                <CharacterPane characters={characters}/>
                            </Col>
                        </Row>
                    </Col>
                    <Col xs={6}>
                        <Row className="p-3">
                            <Col xs={12}>
                                <QuestionPane logGetter={logGetter} characters={characters}/>
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
