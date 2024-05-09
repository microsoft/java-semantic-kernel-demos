import React from 'react';
import {Col, Row} from 'react-bootstrap';
import Form from 'react-bootstrap/Form';
import Character from "../models/Character";
import { Characters } from '../models/Characters';


const CharacterPane: React.FC<{ characters: Characters }> = ({characters}) => {


    const [selectedCharacter, setSelectedCharacter] = React.useState(null as Character | null);

    React.useEffect(() => {
        if (characters.selectedCharacter != null) {
            setSelectedCharacter(characters.selectedCharacter);
        }
    });

    if (selectedCharacter === null) {
        return <div></div>
    }
    return (
        <Form>
            <Row>
                <Col xs={6}>
                    <dl>
                        <dt>Name</dt>
                        <dl>{selectedCharacter!.name}</dl>
                        <dt>Level</dt>
                        <dl>{selectedCharacter!.level}</dl>
                    </dl>
                </Col>
                <Col xs={6}>
                    <dl>
                        <dt>Health</dt>
                        <dl>{selectedCharacter!.health}</dl>
                        <dt>Spells Available</dt>
                        <dl>{selectedCharacter!.spellsAvailable}</dl>
                    </dl>
                </Col>
            </Row>
            <Row>
                <h5>Inventory</h5>
                <Col xs={6}>
                    <ul>
                        {
                            Object.entries(selectedCharacter!.inventory).map((key, value) => {
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
