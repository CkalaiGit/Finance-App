import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import {
   WatchlistResponse,
   TickerExistsResponse,
} from '../models/watchlist.model';

@Injectable({
   providedIn: 'root',
})
export class WatchlistService {
   private readonly baseUrl = 'http://localhost:8081/api/v1/watchlist';

   constructor(private readonly http: HttpClient) {}

   /**
    * Récupère la watchlist complète de l'utilisateur authentifié
    */
   getWatchlist(): Observable<WatchlistResponse> {
      return this.http
         .get<WatchlistResponse>(this.baseUrl)
         .pipe(catchError(this.handleError.bind(this)));
   }

   /**
    * Ajoute un ticker à la watchlist
    * @param ticker Symbole boursier (ex: "AAPL")
    */
   addTicker(ticker: string): Observable<WatchlistResponse> {
      const cleanTicker = ticker.trim().toUpperCase();
      return this.http
         .post<WatchlistResponse>(`${this.baseUrl}/${cleanTicker}`, {})
         .pipe(catchError(this.handleError.bind(this)));
   }

   /**
    * Supprime un ticker de la watchlist
    * @param ticker Symbole boursier (ex: "AAPL")
    */
   removeTicker(ticker: string): Observable<WatchlistResponse> {
      const cleanTicker = ticker.trim().toUpperCase();
      return this.http
         .delete<WatchlistResponse>(`${this.baseUrl}/${cleanTicker}`)
         .pipe(catchError(this.handleError.bind(this)));
   }

   /**
    * Vérifie si un ticker est présent dans la watchlist
    * @param ticker Symbole boursier (ex: "AAPL")
    */
   checkExists(ticker: string): Observable<TickerExistsResponse> {
      const cleanTicker = ticker.trim().toUpperCase();
      return this.http
         .get<TickerExistsResponse>(`${this.baseUrl}/${cleanTicker}/exists`)
         .pipe(catchError(this.handleError.bind(this)));
   }

   private handleError(error: HttpErrorResponse): Observable<never> {
      console.error('Erreur API Watchlist:', error);
      return throwError(
         () => new Error(error.error?.message || 'Erreur API Watchlist'),
      );
   }
}
