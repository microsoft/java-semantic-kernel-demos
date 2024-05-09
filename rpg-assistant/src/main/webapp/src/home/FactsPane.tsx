import React, {FormEvent, useEffect, useState} from 'react';
import {Row} from 'react-bootstrap';
import Form from 'react-bootstrap/Form';
import Character from "../models/Character";
import { Characters } from '../models/Characters';


const FactsPane: React.FC<{
    characters: Characters
}> = ({characters}) => {

    const [fact, setFact]: any = useState(null as String | null);
    const [lastRequest, setLastRequest]: any = useState(null as number | null);

    useEffect(() => {
        if (characters.selectedCharacter != null) {
            setFact(characters.selectedCharacter.facts.facts.join("\n"));
        }
    });

    function updateFact(newValue: string) {
        characters.selectedCharacter!.facts.facts = newValue.split("\n");
        setFact(newValue);

        if (lastRequest) {
            window.clearTimeout(lastRequest);
        }

        var l = setTimeout(() => {
            characters.save(newValue)
        }, 2000);
        setLastRequest(l);
    }

    return (
        <Form>
            <Row>
                <Form.Control as="textarea"
                              contentEditable={true}
                              disabled={false}
                              rows={10}
                              onChange={(e: any) => updateFact(e.currentTarget.value)}
                              value={fact == null ? "" : fact}/>
            </Row>
        </Form>
    )
};

export default FactsPane;
