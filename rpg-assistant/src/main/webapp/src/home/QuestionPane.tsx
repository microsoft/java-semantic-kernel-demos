import React, {FormEvent, useState} from 'react';
import {Row} from 'react-bootstrap';
import Button from 'react-bootstrap/Button';
import Form from 'react-bootstrap/Form';
import {LogGetter} from "../models/Log";
import { Characters } from '../models/Characters';

const QuestionPane: React.FC<{
    logGetter: LogGetter,
    characters: Characters
}> = ({logGetter, characters}) => {
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

        fetch('/assistant/perform/' + characters.selectedCharacter!.uid, requestOptions)
            .then(response => {
                return response.text();
            })
            .then(data => {
                setLoading(false)
                setAnswer(data)
                characters.getById(characters.selectedCharacter!.uid);
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
