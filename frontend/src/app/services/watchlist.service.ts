import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
   WatchlistResponse,
   TickerExistsResponse,
} from '../models/watchlist.model';
import { environment } from '../../environments/environment';

@Injectable({
   providedIn: 'root',
})
export class WatchlistService {
   private readonly http = inject(HttpClient);
   private readonly baseUrl = `${environment.apiUrl}/watchlist`;

   /**
    * Récupère la watchlist complète de l'utilisateur authentifié
    */
   getWatchlist(): Observable<WatchlistResponse> {
      return this.http.get<WatchlistResponse>(this.baseUrl);
   }

   /**
    * Ajoute un ticker à la watchlist
    * @param ticker Symbole boursier (ex: "AAPL")
    */
   addTicker(ticker: string): Observable<WatchlistResponse> {
      const cleanTicker = ticker.trim().toUpperCase();
      return this.http.post<WatchlistResponse>(
         `${this.baseUrl}/${cleanTicker}`,
         {},
      );
   }

   /**
    * Supprime un ticker de la watchlist
    * @param ticker Symbole boursier (ex: "AAPL")
    */
   removeTicker(ticker: string): Observable<WatchlistResponse> {
      const cleanTicker = ticker.trim().toUpperCase();
      return this.http.delete<WatchlistResponse>(
         `${this.baseUrl}/${cleanTicker}`,
      );
   }

   /**
    * Vérifie si un ticker est présent dans la watchlist
    * @param ticker Symbole boursier (ex: "AAPL")
    */
   checkExists(ticker: string): Observable<TickerExistsResponse> {
      const cleanTicker = ticker.trim().toUpperCase();
      return this.http.get<TickerExistsResponse>(
         `${this.baseUrl}/${cleanTicker}/exists`,
      );
   }
}
