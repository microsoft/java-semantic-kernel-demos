import React from 'react';
import {Col, Row} from 'react-bootstrap';
import Form from 'react-bootstrap/Form';
import Character from "../models/Character";


const CharacterPane: React.FC<{ character: Character | null }> = ({character}) => {

    if (character === null) {
        return <div></div>
    }
    return (
        <Form>
            <Row>
                <Col xs={6}>
                    <dl>
                        <dt>Name</dt>
                        <dl>{character.name}</dl>
                        <dt>Level</dt>
                        <dl>{character.level}</dl>
                    </dl>
                </Col>
                <Col xs={6}>
                    <dl>
                        <dt>Health</dt>
                        <dl>{character.health}</dl>
                        <dt>Spells Available</dt>
                        <dl>{character.spellsAvailable}</dl>
                    </dl>
                </Col>
            </Row>
            <Row>
                <h5>Inventory</h5>
                <Col xs={6}>
                    <ul>
                        {
                            Object.entries(character.inventory).map((key, value) => {
                                return <li key={key[0]}>{key[0]}: {key[1]}</li>
                            })
                        }
                    </ul>
                </Col>
            </Row>
        </Form>
    )
};

export default CharacterPane;
