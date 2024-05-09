import React, {useEffect, useState} from 'react';
import {Row} from 'react-bootstrap';
import Form from 'react-bootstrap/Form';
import Character, {CharacterGetter} from "../models/Character";
import { Characters } from "../models/Characters";

const CharSelect: React.FC<{
    characters: Characters
}> = ({characters}) => {

    const [fc, setNames]: any = useState<Map<string, Character>>();
    const [selectedName, setSelectedCharacter]: any = useState<string>();

    let names: Map<string, Character> = fc;

    useEffect(() => {
        characters
            .getAll()
            .then(characters => {
                setSelectedCharacter(characters.selectedCharacter?.uid);
                setNames(characters.characters)
            })
    }, [])

    function getCharacter(id: string) {
        characters.getById(id)
            .then(() => {
                setSelectedCharacter(id);
            })
    }

    return (
        <Row>
            <Form.Select
                value={(selectedName != undefined) ? selectedName : ""}
                onChange={(e: any) => getCharacter(e.currentTarget.value)}>
                {
                    names !== undefined &&
                    Array.from(names.keys()).map(id => {
                        return <option value={id}>{names.get(id)?.name}</option>
                    })
                }
            </Form.Select>
        </Row>
    )
};

export default CharSelect;
