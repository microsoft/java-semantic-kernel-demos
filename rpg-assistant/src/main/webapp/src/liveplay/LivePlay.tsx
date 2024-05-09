import React, {useEffect, useRef, useState} from 'react';
import {Col, Row} from "react-bootstrap";
import Form from "react-bootstrap/Form";
import Button from "react-bootstrap/Button";
import { Characters } from '../models/Characters';
import { LogGetter } from '../models/Log';


const LivePlay: React.FC<{
    characters: Characters,
    logGetter: LogGetter
}> = ({
    characters,
    logGetter
}) => {
        const [lastRequest]: any = useState(null as number | null);
        const [log, setLog]: any = useState([] as string[]);
        const [outputAudio]: any = useState(false);


        const outputAudioRef = useRef(outputAudio);
        const mediaRecorder = useRef(null as MediaRecorder|null);
        const messageIndex = useRef(1);
        const recording = useRef(false);
        const audioContext = new AudioContext();


        function testRecord(e: KeyboardEvent) {
            if(e.altKey && e.ctrlKey) {
                record();
            } else {
                stop()
            }
        }

        useEffect(() =>{
            document.addEventListener('keydown', (e: KeyboardEvent) => {
                testRecord(e);
            })
            document.addEventListener('keyup', (e: KeyboardEvent) => {
                testRecord(e);
            })
        })

        function stop() {
            if(mediaRecorder.current != null) {
                recording.current = false;
                mediaRecorder.current.stop();
                mediaRecorder.current = null;
                window.clearTimeout(lastRequest);
            }
        }


        let openAudio = (): Promise<MediaRecorder> => {
            if(mediaRecorder.current == null) {
                let audioIN = { audio: true };
                return navigator.mediaDevices.getUserMedia(audioIN)
                    .then(function (mediaStreamObj) {
                        return new MediaRecorder(mediaStreamObj);
                    });
            } else {
                return Promise.resolve(mediaRecorder.current)
            }
        }

        function addLogLine(line:string) {
            log.push(line);
            setLog([...log])

            characters.getById(characters.selectedCharacter!.uid);
            logGetter.get();
        }

        // Play the loaded file
        function play(buffer:AudioBuffer) {
            // Create a source node from the buffer
            var source = audioContext.createBufferSource();
            source.buffer = buffer;
            // Connect to the final output node (the speakers)
            source.connect(audioContext.destination);
            // Play immediately
            source.start(0);
        }

        const record = () => {
            
            if(recording.current) {
                return;
            }

            recording.current = true;

            openAudio()
                .then((recorder) => {
                    if(recorder != null) {
                        
                        mediaRecorder.current = recorder
                        messageIndex.current = 0

                        mediaRecorder.current.ondataavailable = function (ev) {
                            // Chunk array to store the audio data 
                            let dataArray:BlobPart[] = [];
                            dataArray.push(ev.data);

                            // blob of type mp3
                            let audioData = new Blob(dataArray, 
                                { 'type': 'audio/mp3;' });
                        
                            const formData = new FormData();
                            formData.append('audio_data', audioData, 'file');
                            formData.append('id', messageIndex.current.toFixed());
                            formData.append('recording', recording.current.toString());
                            formData.append('type', 'mp3');

                            // Your server endpoint to upload audio:
                            const apiUrl = "http://localhost:3000/assistant/upload/audio";

                            const response = fetch(apiUrl, {
                                method: 'POST',
                                cache: 'no-cache',
                                body: formData
                            })
                            .then(response => {
                                return response.json();
                            })
                            .then(response => {
                                if(response.result.length>0) {
                                    addLogLine(response.result);
                                }

                                if(outputAudioRef.current && response.audio.length>0) {
                                    audioContext.decodeAudioData(new Int8Array(response.audio).buffer, function(buffer) {
                                        play(buffer);
                                    });
                                }
                            });

                            messageIndex.current = messageIndex.current + 1;
                        }

                        mediaRecorder.current.start(5000);
                    }
                })
        }

        const handleClick = () => {
            outputAudioRef.current = !outputAudioRef.current;
        }

        return (
            <Row>
                <Col xs={2}>
                    <Row>
                        <Button onClick={record}>Listen</Button>
                    </Row>
                    <Row>
                        <Button onClick={stop}>Stop</Button>
                    </Row>
                    <Row>
                        <div>Describe changes <input type="checkbox" onClick={handleClick} value={outputAudio}/></div>
                        
                    </Row>
                </Col>
                <Col xs={10}>
                    <Form.Control as="textarea"
                                contentEditable={false}
                                disabled={true}
                                rows={30}
                                value={log.join("\n")}/>
                </Col>
            </Row>
        )
            ;
    }
;

export default LivePlay;
