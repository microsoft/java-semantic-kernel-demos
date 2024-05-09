import Character from "./Character";

export class Characters {
    public characters: Map<string, Character> = new Map;
    private setCharacter: any;
    public selectedCharacter: Character|null = null;

    constructor(setCharacter: any) {
        this.setCharacter = setCharacter;
    }

    public save(fact: string): Promise<Character> {

        const requestOptions = {
            method: 'POST',
            headers: {'Content-Type': 'application/text'},
            body: fact
        };

        return fetch('/players/saveFacts/' + this.selectedCharacter!.uid, requestOptions)
            .then(json => {
                return this.getById(this.selectedCharacter!.uid)
            })
    }


    public getAll(): Promise<Characters> {
        const requestOptions = {
            method: 'GET',
            headers: {'Content-Type': 'application/json'}
        };

        return fetch('/players/getAll', requestOptions)
            .then(response => {
                return response.json();
            })
            .then(json => {
                let newCharacters = new Map(Object.entries(json)) as Map<string, Character>;
                this.setCharacter(newCharacters.keys().next().value)
                this.selectedCharacter = newCharacters.values().next().value
                this.characters = newCharacters
                return this;
            })
    }

    public getById(uid: string): Promise<Character> {
        return this.getAll()
        .then((characters) => {
            this.setCharacter(characters.characters.get(uid));
            this.selectedCharacter = characters.characters.get(uid)!;
            return characters.characters.get(uid)!;
        })
    }
}
