import Facts from "./Facts";

class Log {
    log: string[] = [];
}

interface Character {
    uid: string;
    name: string;
    health: number;
    level: number;
    spellsAvailable: number;
    inventory: Map<string, number>;
    facts: Facts;
    log: Log
}

class CharacterGetter {
    constructor(setCharacter: any) {
        this.setCharacter = setCharacter;
    }

    private setCharacter: any;

    public getById(uid: string): Promise<Character> {
        const requestOptions = {
            method: 'GET',
            headers: {'Content-Type': 'application/json'}
        };

        return fetch('/players/info/' + uid, requestOptions)
            .then(response => {
                return response.json();
            })
            .then(json => {
                let char = json as Character;
                this.setCharacter(char);
                return char;
            })
    }
}

export {type Character as default, CharacterGetter};