import React, {FormEvent, useEffect, useState} from 'react';
import {Row} from 'react-bootstrap';
import Form from 'react-bootstrap/Form';
import Character from "../models/Character";


const FactsPane: React.FC<{
    character: Character
}> = ({character}) => {

    const [fact, setFact]: any = useState(null as String | null);
    const [lastRequest, setLastRequest]: any = useState(null as number | null);

    function save(fact: string) {

        const requestOptions = {
            method: 'POST',
            headers: {'Content-Type': 'application/text'},
            body: fact
        };

        fetch('/players/saveFacts/' + character.uid, requestOptions)
    }

    useEffect(() => {
        if (character != null) {
            setFact(character.facts.facts.join("\n"));
        }
    });

    function updateFact(newValue: string) {
        character.facts.facts = newValue.split("\n");
        setFact(newValue);

        if (lastRequest) {
            window.clearTimeout(lastRequest);
        }

        var l = setTimeout(() => {
            save(newValue)
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
