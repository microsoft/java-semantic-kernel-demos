import React from 'react';
import {Row} from 'react-bootstrap';
import Character from "../models/Character";
import Form from "react-bootstrap/Form";
import { Characters } from '../models/Characters';


const CharacterLogPane: React.FC<{ characters: Characters }> = ({characters}) => {

    const [character, setSelectedCharacter]: any = React.useState(null as Character | null);

    React.useEffect(() => {
        if (characters.selectedCharacter != null) {
            setSelectedCharacter(characters.selectedCharacter);
        }
    });

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
