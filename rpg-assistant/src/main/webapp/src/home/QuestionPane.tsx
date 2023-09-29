import React, {FormEvent, useState} from 'react';
import {Row} from 'react-bootstrap';
import Button from 'react-bootstrap/Button';
import Form from 'react-bootstrap/Form';
import Character, {CharacterGetter} from "../models/Character";
import {LogGetter} from "../models/Log";

const QuestionPane: React.FC<{
    logGetter: LogGetter,
    character: Character,
    loadCharacter: CharacterGetter
}> = ({logGetter, character, loadCharacter}) => {
    const [question, setQuestion]: any = useState('');
    const [answer, setAnswer]: any = useState('');
    const [loading, setLoading]: any = useState(false);

    function askQuestion(event: FormEvent) {
        event.preventDefault()

        const requestOptions = {
            method: 'POST',
            headers: {'Content-Type': 'application/text'},
            body: question
        };
        setLoading(true)

        fetch('/assistant/perform/' + character.uid, requestOptions)
            .then(response => {
                return response.text();
            })
            .then(data => {
                setLoading(false)
                setAnswer(data)
                loadCharacter.getById(character.uid);
                logGetter.get();
            });
    }

    return (
        <Form onSubmit={(event) => askQuestion(event)}>
            <Row>
                <Form.Control as="input"
                              value={question}
                              onChange={(e: any) => setQuestion(e.currentTarget.value)}/>
                <Button variant="primary" type={"submit"}>
                    <span className="sr-only">Ask Question/Give Instruction  </span>
                    <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true"
                          hidden={!loading}></span>
                </Button>
            </Row>
            <Row>
                <Form.Control as="textarea"
                              contentEditable={false}
                              disabled={true}
                              rows={10}
                              value={answer}/>
            </Row>
        </Form>
    )
};

export default QuestionPane;
