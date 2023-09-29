class Log {
    log: string[] = [];
}

class LogGetter {
    constructor(setLog: any) {
        this.setLog = setLog;
    }

    private setLog: any;

    public get(): Promise<Log> {
        const requestOptions = {
            method: 'GET',
            headers: {'Content-Type': 'application/json'}
        };

        return fetch('/log/world', requestOptions)
            .then(response => {
                return response.json();
            })
            .then(json => {
                let data = json as Log;
                this.setLog(data);
                return data;
            })
    }
}

export {Log, LogGetter};