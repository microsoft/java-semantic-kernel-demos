import React from 'react';
import {Row} from 'react-bootstrap';
import Character from "../models/Character";
import Form from "react-bootstrap/Form";


const CharacterLogPane: React.FC<{ character: Character | null }> = ({character}) => {

    if (character === null) {
        return <div></div>
    }
    return (

        <Row>
            <Form.Control as="textarea"
                          contentEditable={false}
                          disabled={true}
                          rows={10}
                          value={character.log.log.join("\n")}/>
        </Row>
    )
};

export default CharacterLogPane;
